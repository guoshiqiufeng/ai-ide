package com.aiide.repository;

import com.aiide.entity.Prompt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface PromptRepository extends JpaRepository<Prompt, Long> {
    Optional<Prompt> findByType(String type);
    List<Prompt> findByIsEnabledTrue();
    List<Prompt> findAllByOrderByIdAsc();
}
