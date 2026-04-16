package com.aiide.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiTestResponse {
    private Boolean success;
    private String message;
    private String response;
}
