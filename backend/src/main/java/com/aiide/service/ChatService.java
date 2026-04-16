package com.aiide.service;

import com.aiide.dto.*;
import com.aiide.entity.ChatSession;
import reactor.core.publisher.Flux;
import java.util.List;

public interface ChatService {
    R<List<ChatSession>> listSessions();
    R<ChatSession> createSession(String title);
    R<Void> deleteSession(Long sessionId);
    Flux<String> chat(ChatRequest request);
    R<List<TaskDTO>> getTasksByMessage(Long messageId);
}
