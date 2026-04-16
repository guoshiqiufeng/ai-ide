package com.aiide.repository;

import com.aiide.entity.ApiSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ApiSettingRepository extends JpaRepository<ApiSetting, Long> {
    Optional<ApiSetting> findByIsActiveTrue();
}
