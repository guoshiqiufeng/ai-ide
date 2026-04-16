package com.aiide.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskDTO {
    private Long id;
    private String type;
    private String status;
    private String input;
    private String result;
    private Integer sortOrder;
}
