package com.aiide.controller;

import com.aiide.dto.*;
import com.aiide.entity.ChatSession;
import com.aiide.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/sessions")
    public R<List<ChatSession>> listSessions() {
        return chatService.listSessions();
    }

    @PostMapping("/sessions")
    public R<ChatSession> createSession(@RequestBody ChatRequest request) {
        return chatService.createSession(request.getMessage());
    }

    @DeleteMapping("/sessions/{sessionId}")
    public R<Void> deleteSession(@PathVariable Long sessionId) {
        return chatService.deleteSession(sessionId);
    }

    @PostMapping(value = "/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody ChatRequest request) {
        return chatService.chat(request);
    }

    @GetMapping("/tasks/{messageId}")
    public R<List<TaskDTO>> getTasksByMessage(@PathVariable Long messageId) {
        return chatService.getTasksByMessage(messageId);
    }
}
