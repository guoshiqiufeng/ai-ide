package com.aiide.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelListResponse {
    private Boolean success;
    private List<String> models;
    private String message;
}
