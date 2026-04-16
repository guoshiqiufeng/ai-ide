package com.aiide.service.impl;

import com.aiide.dto.R;
import com.aiide.entity.UserMessage;
import com.aiide.repository.UserMessageRepository;
import com.aiide.service.UserMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserMessageServiceImpl implements UserMessageService {

    private final UserMessageRepository userMessageRepository;

    @Override
    public R<List<UserMessage>> listMessages() {
        return R.ok(userMessageRepository.findAllByOrderByCreatedAtDesc());
    }

    @Override
    public R<List<UserMessage>> listMessagesBySession(Long sessionId) {
        return R.ok(userMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId));
    }

    @Override
    @Transactional
    public R<UserMessage> saveMessage(UserMessage message) {
        return R.ok(userMessageRepository.save(message));
    }

    @Override
    @Transactional
    public R<Void> deleteMessage(Long id) {
        if (userMessageRepository.existsById(id)) {
            userMessageRepository.deleteById(id);
            return R.ok();
        }
        return R.error("Message not found");
    }

    @Override
    @Transactional
    public R<Void> deleteAllMessages() {
        userMessageRepository.deleteAll();
        return R.ok();
    }
}
