package com.aiide.service;

import com.aiide.dto.*;

public interface ApiSettingService {
    R<ApiSettingDTO> getActiveSetting();
    R<ApiSettingDTO> saveSetting(ApiSettingDTO dto);
    R<ModelListResponse> fetchModels(String provider, String apiKey, String apiUrl);
    R<ApiTestResponse> testApi(ApiTestRequest request);
}
