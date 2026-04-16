package com.aiide.service.impl;

import com.aiide.dto.*;
import com.aiide.entity.*;
import com.aiide.repository.*;
import com.aiide.service.AiService;
import com.aiide.service.ChatService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final UserMessageRepository userMessageRepository;
    private final TaskRepository taskRepository;
    private final ApiSettingRepository apiSettingRepository;
    private final PromptRepository promptRepository;
    private final AiService aiService;
    private final ObjectMapper objectMapper;

    @Override
    public R<List<ChatSession>> listSessions() {
        return R.ok(chatSessionRepository.findAllByOrderByUpdatedAtDesc());
    }

    @Override
    @Transactional
    public R<ChatSession> createSession(String title) {
        ChatSession session = ChatSession.builder()
                .title(title)
                .build();
        return R.ok(chatSessionRepository.save(session));
    }

    @Override
    @Transactional
    public R<Void> deleteSession(Long sessionId) {
        if (chatSessionRepository.existsById(sessionId)) {
            // Delete related messages and tasks
            List<UserMessage> messages = userMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
            for (UserMessage msg : messages) {
                taskRepository.findByMessageIdOrderBySortOrderAsc(msg.getId())
                        .forEach(task -> taskRepository.deleteById(task.getId()));
            }
            userMessageRepository.deleteAll(messages);
            chatSessionRepository.deleteById(sessionId);
            return R.ok();
        }
        return R.error("Session not found");
    }

    @Override
    public Flux<String> chat(ChatRequest request) {
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();

        CompletableFuture.runAsync(() -> {
            try {
                processChatAsync(request, sink);
            } catch (Exception e) {
                log.error("Chat processing failed", e);
                sink.tryEmitNext(buildSSEData("error", "Processing failed: " + e.getMessage()));
                sink.tryEmitComplete();
            }
        });

        return sink.asFlux();
    }

    private void processChatAsync(ChatRequest request, Sinks.Many<String> sink) {
        // Get active API setting
        ApiSetting apiSetting = apiSettingRepository.findByIsActiveTrue()
                .orElseThrow(() -> new RuntimeException("No active API setting found. Please configure API settings first."));

        // Ensure session exists
        Long sessionId = request.getSessionId();
        if (sessionId == null) {
            ChatSession session = chatSessionRepository.save(
                    ChatSession.builder().title(request.getMessage().substring(0, Math.min(50, request.getMessage().length()))).build()
            );
            sessionId = session.getId();
        }

        // Save user message
        UserMessage userMsg = userMessageRepository.save(UserMessage.builder()
                .content(request.getMessage())
                .role("USER")
                .sessionId(sessionId)
                .build());

        sink.tryEmitNext(buildSSEData("session", "{\"sessionId\":" + sessionId + ",\"messageId\":" + userMsg.getId() + "}"));

        // Step 1: Analyze the task
        sink.tryEmitNext(buildSSEData("status", "Analyzing your request..."));

        Prompt analysisPrompt = promptRepository.findByType("TASK_ANALYSIS")
                .filter(p -> p.getIsEnabled())
                .orElseThrow(() -> new RuntimeException("Task analysis prompt not found or disabled"));

        String analysisResult = aiService.chat(
                apiSetting.getProvider(), apiSetting.getApiKey(), apiSetting.getApiUrl(),
                apiSetting.getModelName(), apiSetting.getTemperature(), apiSetting.getMaxTokens(),
                analysisPrompt.getContent(), request.getMessage()
        );

        sink.tryEmitNext(buildSSEData("analysis", analysisResult));

        try {
            JsonNode analysis = objectMapper.readTree(analysisResult);
            boolean needsCoding = analysis.has("needsCoding") && analysis.get("needsCoding").asBoolean();

            if (!needsCoding) {
                // Regular conversation response
                String response = analysis.has("response") ? analysis.get("response").asText() : analysisResult;
                userMessageRepository.save(UserMessage.builder()
                        .content(response)
                        .role("ASSISTANT")
                        .sessionId(request.getSessionId() != null ? request.getSessionId() : userMsg.getSessionId())
                        .build());
                sink.tryEmitNext(buildSSEData("response", response));
                sink.tryEmitComplete();
                return;
            }

            // Create tasks based on analysis
            JsonNode tasks = analysis.get("tasks");
            List<Task> createdTasks = new ArrayList<>();
            int order = 0;

            String[] taskTypes = {"database", "api", "frontend", "backend"};
            String[] taskTypeEnums = {"DATABASE", "API", "FRONTEND", "BACKEND"};

            for (int i = 0; i < taskTypes.length; i++) {
                if (tasks.has(taskTypes[i]) && !tasks.get(taskTypes[i]).isNull()) {
                    Task task = taskRepository.save(Task.builder()
                            .sessionId(userMsg.getSessionId())
                            .messageId(userMsg.getId())
                            .type(taskTypeEnums[i])
                            .status("PENDING")
                            .input(tasks.get(taskTypes[i]).asText())
                            .sortOrder(order++)
                            .build());
                    createdTasks.add(task);
                }
            }

            sink.tryEmitNext(buildSSEData("tasks", objectMapper.writeValueAsString(
                    createdTasks.stream().map(this::toTaskDTO).collect(Collectors.toList())
            )));

            // Process tasks in order
            String databaseResult = null;
            String apiResult = null;

            for (Task task : createdTasks) {
                if ("DATABASE".equals(task.getType())) {
                    sink.tryEmitNext(buildSSEData("task_start", "{\"taskId\":" + task.getId() + ",\"type\":\"DATABASE\"}"));
                    task.setStatus("IN_PROGRESS");
                    taskRepository.save(task);

                    databaseResult = processTask(apiSetting, "DATABASE_TASK", task.getInput(), null, null);
                    task.setResult(databaseResult);
                    task.setStatus("COMPLETED");
                    taskRepository.save(task);

                    sink.tryEmitNext(buildSSEData("task_complete", "{\"taskId\":" + task.getId() + ",\"type\":\"DATABASE\",\"result\":" + objectMapper.writeValueAsString(databaseResult) + "}"));
                }
            }

            for (Task task : createdTasks) {
                if ("API".equals(task.getType())) {
                    sink.tryEmitNext(buildSSEData("task_start", "{\"taskId\":" + task.getId() + ",\"type\":\"API\"}"));
                    task.setStatus("IN_PROGRESS");
                    taskRepository.save(task);

                    apiResult = processTask(apiSetting, "API_TASK", task.getInput(), databaseResult, null);
                    task.setResult(apiResult);
                    task.setStatus("COMPLETED");
                    taskRepository.save(task);

                    sink.tryEmitNext(buildSSEData("task_complete", "{\"taskId\":" + task.getId() + ",\"type\":\"API\",\"result\":" + objectMapper.writeValueAsString(apiResult) + "}"));
                }
            }

            // Process frontend and backend tasks in parallel
            String finalDatabaseResult = databaseResult;
            String finalApiResult = apiResult;
            List<CompletableFuture<Void>> parallelTasks = new ArrayList<>();

            for (Task task : createdTasks) {
                if ("FRONTEND".equals(task.getType()) || "BACKEND".equals(task.getType())) {
                    String taskType = task.getType();
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        try {
                            sink.tryEmitNext(buildSSEData("task_start", "{\"taskId\":" + task.getId() + ",\"type\":\"" + taskType + "\"}"));
                            task.setStatus("IN_PROGRESS");
                            taskRepository.save(task);

                            String promptType = taskType + "_TASK";
                            String result = processTask(apiSetting, promptType, task.getInput(), finalDatabaseResult, finalApiResult);
                            task.setResult(result);
                            task.setStatus("COMPLETED");
                            taskRepository.save(task);

                            sink.tryEmitNext(buildSSEData("task_complete", "{\"taskId\":" + task.getId() + ",\"type\":\"" + taskType + "\",\"result\":" + objectMapper.writeValueAsString(result) + "}"));
                        } catch (Exception e) {
                            log.error("Task {} failed", taskType, e);
                            task.setStatus("FAILED");
                            task.setResult("Error: " + e.getMessage());
                            taskRepository.save(task);
                            try {
                                String errorJson = objectMapper.writeValueAsString(e.getMessage());
                                sink.tryEmitNext(buildSSEData("task_error", "{\"taskId\":" + task.getId() + ",\"type\":\"" + taskType + "\",\"error\":" + errorJson + "}"));
                            } catch (Exception ex) {
                                sink.tryEmitNext(buildSSEData("task_error", "{\"taskId\":" + task.getId() + ",\"type\":\"" + taskType + "\",\"error\":\"serialization error\"}"));
                            }
                        }
                    });
                    parallelTasks.add(future);
                }
            }

            // Wait for parallel tasks to complete
            CompletableFuture.allOf(parallelTasks.toArray(new CompletableFuture[0])).join();

            // Save assistant response
            StringBuilder fullResponse = new StringBuilder();
            for (Task task : createdTasks) {
                fullResponse.append("## ").append(task.getType()).append(" Task\n\n");
                fullResponse.append(task.getResult() != null ? task.getResult() : "No result").append("\n\n");
            }

            userMessageRepository.save(UserMessage.builder()
                    .content(fullResponse.toString())
                    .role("ASSISTANT")
                    .sessionId(userMsg.getSessionId())
                    .build());

            sink.tryEmitNext(buildSSEData("complete", "All tasks completed"));
            sink.tryEmitComplete();

        } catch (Exception e) {
            log.error("Failed to process task analysis", e);
            // If analysis parsing fails, treat as regular response
            userMessageRepository.save(UserMessage.builder()
                    .content(analysisResult)
                    .role("ASSISTANT")
                    .sessionId(userMsg.getSessionId())
                    .build());
            sink.tryEmitNext(buildSSEData("response", analysisResult));
            sink.tryEmitComplete();
        }
    }

    private String processTask(ApiSetting apiSetting, String promptType, String taskDescription,
                               String databaseResult, String apiResult) {
        Prompt prompt = promptRepository.findByType(promptType)
                .filter(p -> p.getIsEnabled())
                .orElseThrow(() -> new RuntimeException("Prompt not found or disabled: " + promptType));

        String systemPrompt = prompt.getContent()
                .replace("{task_description}", taskDescription != null ? taskDescription : "")
                .replace("{database_result}", databaseResult != null ? databaseResult : "N/A")
                .replace("{api_result}", apiResult != null ? apiResult : "N/A");

        return aiService.chat(
                apiSetting.getProvider(), apiSetting.getApiKey(), apiSetting.getApiUrl(),
                apiSetting.getModelName(), apiSetting.getTemperature(), apiSetting.getMaxTokens(),
                systemPrompt, taskDescription
        );
    }

    @Override
    public R<List<TaskDTO>> getTasksByMessage(Long messageId) {
        List<TaskDTO> tasks = taskRepository.findByMessageIdOrderBySortOrderAsc(messageId)
                .stream()
                .map(this::toTaskDTO)
                .collect(Collectors.toList());
        return R.ok(tasks);
    }

    private TaskDTO toTaskDTO(Task task) {
        return TaskDTO.builder()
                .id(task.getId())
                .type(task.getType())
                .status(task.getStatus())
                .input(task.getInput())
                .result(task.getResult())
                .sortOrder(task.getSortOrder())
                .build();
    }

    private String buildSSEData(String event, String data) {
        try {
            if (data.startsWith("{") || data.startsWith("[")) {
                return "{\"event\":\"" + event + "\",\"data\":" + data + "}";
            } else {
                return "{\"event\":\"" + event + "\",\"data\":" + objectMapper.writeValueAsString(data) + "}";
            }
        } catch (Exception e) {
            log.error("Failed to build SSE data", e);
            return "{\"event\":\"error\",\"data\":\"Failed to serialize data\"}";
        }
    }
}
