package com.aiide.repository;

import com.aiide.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findBySessionIdOrderBySortOrderAsc(Long sessionId);
    List<Task> findByMessageIdOrderBySortOrderAsc(Long messageId);
}
