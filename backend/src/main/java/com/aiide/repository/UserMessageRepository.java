package com.aiide.repository;

import com.aiide.entity.UserMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserMessageRepository extends JpaRepository<UserMessage, Long> {
    List<UserMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
    List<UserMessage> findAllByOrderByCreatedAtDesc();
}
