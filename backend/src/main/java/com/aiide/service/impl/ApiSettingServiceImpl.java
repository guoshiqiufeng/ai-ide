package com.aiide.service.impl;

import com.aiide.dto.*;
import com.aiide.entity.ApiSetting;
import com.aiide.repository.ApiSettingRepository;
import com.aiide.service.AiService;
import com.aiide.service.ApiSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiSettingServiceImpl implements ApiSettingService {

    private final ApiSettingRepository apiSettingRepository;
    private final AiService aiService;

    @Override
    public R<ApiSettingDTO> getActiveSetting() {
        return apiSettingRepository.findByIsActiveTrue()
                .map(this::toDTO)
                .map(R::ok)
                .orElse(R.ok(null));
    }

    @Override
    @Transactional
    public R<ApiSettingDTO> saveSetting(ApiSettingDTO dto) {
        ApiSetting setting = apiSettingRepository.findByIsActiveTrue()
                .orElse(new ApiSetting());

        setting.setProvider(dto.getProvider());
        setting.setApiKey(dto.getApiKey());
        setting.setApiUrl(dto.getApiUrl());
        setting.setModelName(dto.getModelName());
        setting.setTemperature(dto.getTemperature());
        setting.setMaxTokens(dto.getMaxTokens());
        setting.setIsActive(true);

        setting = apiSettingRepository.save(setting);
        return R.ok(toDTO(setting));
    }

    @Override
    public R<ModelListResponse> fetchModels(String provider, String apiKey, String apiUrl) {
        ModelListResponse response = aiService.fetchModels(provider, apiKey, apiUrl);
        return R.ok(response);
    }

    @Override
    public R<ApiTestResponse> testApi(ApiTestRequest request) {
        ApiTestResponse response = aiService.testApi(request);
        return R.ok(response);
    }

    private ApiSettingDTO toDTO(ApiSetting entity) {
        return ApiSettingDTO.builder()
                .id(entity.getId())
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
