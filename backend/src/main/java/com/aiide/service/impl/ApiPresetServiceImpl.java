package com.aiide.service.impl;

import com.aiide.dto.*;
import com.aiide.entity.ApiPreset;
import com.aiide.entity.ApiSetting;
import com.aiide.repository.ApiPresetRepository;
import com.aiide.repository.ApiSettingRepository;
import com.aiide.service.AiService;
import com.aiide.service.ApiPresetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiPresetServiceImpl implements ApiPresetService {

    private final ApiPresetRepository apiPresetRepository;
    private final ApiSettingRepository apiSettingRepository;
    private final AiService aiService;

    @Override
    public R<List<ApiPresetDTO>> listPresets() {
        List<ApiPresetDTO> presets = apiPresetRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return R.ok(presets);
    }

    @Override
    public R<ApiPresetDTO> getPreset(Long id) {
        return apiPresetRepository.findById(id)
                .map(this::toDTO)
                .map(R::ok)
                .orElse(R.error("Preset not found"));
    }

    @Override
    @Transactional
    public R<ApiPresetDTO> createPreset(ApiPresetDTO dto) {
        ApiPreset preset = ApiPreset.builder()
                .name(dto.getName())
                .provider(dto.getProvider())
                .apiKey(dto.getApiKey())
                .apiUrl(dto.getApiUrl())
                .modelName(dto.getModelName())
                .temperature(dto.getTemperature())
                .maxTokens(dto.getMaxTokens())
                .isActive(false)
                .build();
        preset = apiPresetRepository.save(preset);
        return R.ok(toDTO(preset));
    }

    @Override
    @Transactional
    public R<ApiPresetDTO> updatePreset(Long id, ApiPresetDTO dto) {
        return apiPresetRepository.findById(id)
                .map(preset -> {
                    preset.setName(dto.getName());
                    preset.setProvider(dto.getProvider());
                    preset.setApiKey(dto.getApiKey());
                    preset.setApiUrl(dto.getApiUrl());
                    preset.setModelName(dto.getModelName());
                    preset.setTemperature(dto.getTemperature());
                    preset.setMaxTokens(dto.getMaxTokens());
                    return R.ok(toDTO(apiPresetRepository.save(preset)));
                })
                .orElse(R.error("Preset not found"));
    }

    @Override
    @Transactional
    public R<Void> deletePreset(Long id) {
        if (apiPresetRepository.existsById(id)) {
            apiPresetRepository.deleteById(id);
            return R.ok();
        }
        return R.error("Preset not found");
    }

    @Override
    @Transactional
    public R<ApiPresetDTO> activatePreset(Long id) {
        return apiPresetRepository.findById(id)
                .map(preset -> {
                    // Deactivate all presets
                    apiPresetRepository.findAll().forEach(p -> {
                        p.setIsActive(false);
                        apiPresetRepository.save(p);
                    });
                    // Activate selected preset
                    preset.setIsActive(true);
                    apiPresetRepository.save(preset);

                    // Also update the active API setting
                    ApiSetting setting = apiSettingRepository.findByIsActiveTrue()
                            .orElse(ApiSetting.builder().build());
                    setting.setProvider(preset.getProvider());
                    setting.setApiKey(preset.getApiKey());
                    setting.setApiUrl(preset.getApiUrl());
                    setting.setModelName(preset.getModelName());
                    setting.setTemperature(preset.getTemperature());
                    setting.setMaxTokens(preset.getMaxTokens());
                    setting.setIsActive(true);
                    apiSettingRepository.save(setting);

                    return R.ok(toDTO(preset));
                })
                .orElse(R.error("Preset not found"));
    }

    @Override
    public R<ApiTestResponse> testPreset(Long id) {
        return apiPresetRepository.findById(id)
                .map(preset -> {
                    ApiTestRequest request = ApiTestRequest.builder()
                            .provider(preset.getProvider())
                            .apiKey(preset.getApiKey())
                            .apiUrl(preset.getApiUrl())
                            .modelName(preset.getModelName())
                            .temperature(preset.getTemperature())
                            .maxTokens(preset.getMaxTokens())
                            .testMessage("Hello, this is a test. Please respond briefly.")
                            .build();
                    return R.ok(aiService.testApi(request));
                })
                .orElse(R.error("Preset not found"));
    }

    private ApiPresetDTO toDTO(ApiPreset entity) {
        return ApiPresetDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .provider(entity.getProvider())
                .apiKey(entity.getApiKey())
                .apiUrl(entity.getApiUrl())
                .modelName(entity.getModelName())
                .temperature(entity.getTemperature())
                .maxTokens(entity.getMaxTokens())
                .isActive(entity.getIsActive())
                .build();
    }
}
