package com.aiide.repository;

import com.aiide.entity.ApiPreset;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ApiPresetRepository extends JpaRepository<ApiPreset, Long> {
    Optional<ApiPreset> findByIsActiveTrue();
    List<ApiPreset> findAllByOrderByCreatedAtDesc();
}
