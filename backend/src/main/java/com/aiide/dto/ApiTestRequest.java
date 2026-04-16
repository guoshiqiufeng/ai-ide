package com.aiide.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiTestRequest {
    private String provider;
    private String apiKey;
    private String apiUrl;
    private String modelName;
    private Double temperature;
    private Integer maxTokens;
    private String testMessage;
}
