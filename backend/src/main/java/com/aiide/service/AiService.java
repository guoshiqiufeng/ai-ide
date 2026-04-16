package com.aiide.service;

import com.aiide.dto.ApiTestRequest;
import com.aiide.dto.ApiTestResponse;
import com.aiide.dto.ModelListResponse;
import reactor.core.publisher.Flux;

public interface AiService {

    ModelListResponse fetchModels(String provider, String apiKey, String apiUrl);

    ApiTestResponse testApi(ApiTestRequest request);

    String chat(String provider, String apiKey, String apiUrl, String model,
                Double temperature, Integer maxTokens, String systemPrompt, String userMessage);

    Flux<String> chatStream(String provider, String apiKey, String apiUrl, String model,
                            Double temperature, Integer maxTokens, String systemPrompt, String userMessage);
}
