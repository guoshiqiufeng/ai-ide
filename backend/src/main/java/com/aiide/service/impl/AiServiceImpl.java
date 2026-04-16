package com.aiide.service.impl;

import com.aiide.dto.ApiTestRequest;
import com.aiide.dto.ApiTestResponse;
import com.aiide.dto.ModelListResponse;
import com.aiide.service.AiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final ObjectMapper objectMapper;

    private WebClient buildWebClient(String baseUrl, String apiKey) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
    }

    @Override
    public ModelListResponse fetchModels(String provider, String apiKey, String apiUrl) {
        try {
            WebClient client = buildWebClient(apiUrl, apiKey);
            String modelsEndpoint = "GOOGLE_GEMINI".equals(provider)
                    ? "/v1beta/models?key=" + apiKey
                    : "/v1/models";

            String response;
            if ("GOOGLE_GEMINI".equals(provider)) {
                response = WebClient.builder()
                        .baseUrl(apiUrl)
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                        .build()
                        .get()
                        .uri(modelsEndpoint)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
            } else {
                response = client.get()
                        .uri("/v1/models")
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
            }

            List<String> models = parseModels(provider, response);
            return ModelListResponse.builder()
                    .success(true)
                    .models(models)
                    .message("Models fetched successfully")
                    .build();
        } catch (Exception e) {
            log.error("Failed to fetch models", e);
            return ModelListResponse.builder()
                    .success(false)
                    .models(Collections.emptyList())
                    .message("Failed to fetch models: " + e.getMessage())
                    .build();
        }
    }

    private List<String> parseModels(String provider, String response) {
        List<String> models = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(response);
            if ("GOOGLE_GEMINI".equals(provider)) {
                JsonNode modelsNode = root.get("models");
                if (modelsNode != null && modelsNode.isArray()) {
                    for (JsonNode model : modelsNode) {
                        String name = model.get("name").asText();
                        models.add(name.replace("models/", ""));
                    }
                }
            } else {
                JsonNode dataNode = root.get("data");
                if (dataNode != null && dataNode.isArray()) {
                    for (JsonNode model : dataNode) {
                        models.add(model.get("id").asText());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse models response", e);
        }
        return models;
    }

    @Override
    public ApiTestResponse testApi(ApiTestRequest request) {
        try {
            String result = chat(
                    request.getProvider(),
                    request.getApiKey(),
                    request.getApiUrl(),
                    request.getModelName(),
                    request.getTemperature(),
                    request.getMaxTokens(),
                    "You are a helpful assistant.",
                    request.getTestMessage() != null ? request.getTestMessage() : "Hello, this is a test message. Please respond briefly."
            );
            return ApiTestResponse.builder()
                    .success(true)
                    .message("API test successful")
                    .response(result)
                    .build();
        } catch (Exception e) {
            log.error("API test failed", e);
            return ApiTestResponse.builder()
                    .success(false)
                    .message("API test failed: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public String chat(String provider, String apiKey, String apiUrl, String model,
                       Double temperature, Integer maxTokens, String systemPrompt, String userMessage) {
        try {
            if ("GOOGLE_GEMINI".equals(provider)) {
                return chatGemini(apiKey, apiUrl, model, temperature, maxTokens, systemPrompt, userMessage);
            } else {
                return chatOpenAI(apiKey, apiUrl, model, temperature, maxTokens, systemPrompt, userMessage);
            }
        } catch (Exception e) {
            log.error("Chat failed", e);
            throw new RuntimeException("Chat failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Flux<String> chatStream(String provider, String apiKey, String apiUrl, String model,
                                   Double temperature, Integer maxTokens, String systemPrompt, String userMessage) {
        if ("GOOGLE_GEMINI".equals(provider)) {
            return chatStreamGemini(apiKey, apiUrl, model, temperature, maxTokens, systemPrompt, userMessage);
        } else {
            return chatStreamOpenAI(apiKey, apiUrl, model, temperature, maxTokens, systemPrompt, userMessage);
        }
    }

    private String chatOpenAI(String apiKey, String apiUrl, String model,
                              Double temperature, Integer maxTokens, String systemPrompt, String userMessage) {
        WebClient client = buildWebClient(apiUrl, apiKey);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        List<Map<String, String>> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        messages.add(Map.of("role", "user", "content", userMessage));
        body.put("messages", messages);
        if (temperature != null) body.put("temperature", temperature);
        if (maxTokens != null) body.put("max_tokens", maxTokens);

        String response = client.post()
                .uri("/v1/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        try {
            JsonNode root = objectMapper.readTree(response);
            return root.get("choices").get(0).get("message").get("content").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse OpenAI response: " + response, e);
        }
    }

    private String chatGemini(String apiKey, String apiUrl, String model,
                              Double temperature, Integer maxTokens, String systemPrompt, String userMessage) {
        WebClient client = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();

        Map<String, Object> body = new LinkedHashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))));
        }

        contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", userMessage))));
        body.put("contents", contents);

        Map<String, Object> generationConfig = new LinkedHashMap<>();
        if (temperature != null) generationConfig.put("temperature", temperature);
        if (maxTokens != null) generationConfig.put("maxOutputTokens", maxTokens);
        if (!generationConfig.isEmpty()) body.put("generationConfig", generationConfig);

        String response = client.post()
                .uri("/v1beta/models/" + model + ":generateContent?key=" + apiKey)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        try {
            JsonNode root = objectMapper.readTree(response);
            return root.get("candidates").get(0).get("content").get("parts").get(0).get("text").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Gemini response: " + response, e);
        }
    }

    private Flux<String> chatStreamOpenAI(String apiKey, String apiUrl, String model,
                                          Double temperature, Integer maxTokens, String systemPrompt, String userMessage) {
        WebClient client = buildWebClient(apiUrl, apiKey);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("stream", true);
        List<Map<String, String>> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        messages.add(Map.of("role", "user", "content", userMessage));
        body.put("messages", messages);
        if (temperature != null) body.put("temperature", temperature);
        if (maxTokens != null) body.put("max_tokens", maxTokens);

        return client.post()
                .uri("/v1/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(line -> !line.equals("[DONE]"))
                .map(line -> {
                    try {
                        JsonNode root = objectMapper.readTree(line);
                        JsonNode delta = root.get("choices").get(0).get("delta");
                        if (delta != null && delta.has("content")) {
                            return delta.get("content").asText();
                        }
                        return "";
                    } catch (Exception e) {
                        return "";
                    }
                })
                .filter(s -> !s.isEmpty());
    }

    private Flux<String> chatStreamGemini(String apiKey, String apiUrl, String model,
                                          Double temperature, Integer maxTokens, String systemPrompt, String userMessage) {
        // Gemini streaming uses a different approach; for simplicity, fall back to non-streaming
        return Flux.just(chatGemini(apiKey, apiUrl, model, temperature, maxTokens, systemPrompt, userMessage));
    }
}
