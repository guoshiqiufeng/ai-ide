package com.aiide.service;

import com.aiide.dto.R;
import com.aiide.entity.UserMessage;
import java.util.List;

public interface UserMessageService {
    R<List<UserMessage>> listMessages();
    R<List<UserMessage>> listMessagesBySession(Long sessionId);
    R<UserMessage> saveMessage(UserMessage message);
    R<Void> deleteMessage(Long id);
    R<Void> deleteAllMessages();
}
