package com.aiide.controller;

import com.aiide.dto.*;
import com.aiide.service.ApiSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class ApiSettingController {

    private final ApiSettingService apiSettingService;

    @GetMapping
    public R<ApiSettingDTO> getActiveSetting() {
        return apiSettingService.getActiveSetting();
    }

    @PostMapping
    public R<ApiSettingDTO> saveSetting(@RequestBody ApiSettingDTO dto) {
        return apiSettingService.saveSetting(dto);
    }

    @PostMapping("/fetch-models")
    public R<ModelListResponse> fetchModels(@RequestBody ApiSettingDTO dto) {
        return apiSettingService.fetchModels(dto.getProvider(), dto.getApiKey(), dto.getApiUrl());
    }

    @PostMapping("/test")
    public R<ApiTestResponse> testApi(@RequestBody ApiTestRequest request) {
        return apiSettingService.testApi(request);
    }
}
