package com.aiide.controller;

import com.aiide.dto.R;
import com.aiide.entity.UserMessage;
import com.aiide.service.UserMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class UserMessageController {

    private final UserMessageService userMessageService;

    @GetMapping
    public R<List<UserMessage>> listMessages() {
        return userMessageService.listMessages();
    }

    @GetMapping("/session/{sessionId}")
    public R<List<UserMessage>> listMessagesBySession(@PathVariable Long sessionId) {
        return userMessageService.listMessagesBySession(sessionId);
    }

    @PostMapping
    public R<UserMessage> saveMessage(@RequestBody UserMessage message) {
        return userMessageService.saveMessage(message);
    }

    @DeleteMapping("/{id}")
    public R<Void> deleteMessage(@PathVariable Long id) {
        return userMessageService.deleteMessage(id);
    }

    @DeleteMapping
    public R<Void> deleteAllMessages() {
        return userMessageService.deleteAllMessages();
    }
}
