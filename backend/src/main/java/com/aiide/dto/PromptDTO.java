package com.aiide.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromptDTO {
    private Long id;
    private String type;
    private String name;
    private String content;
    private Boolean isEnabled;
}
