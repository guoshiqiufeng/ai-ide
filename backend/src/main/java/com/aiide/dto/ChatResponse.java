package com.aiide.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponse {
    private Long sessionId;
    private Long messageId;
    private String content;
    private Boolean needsCoding;
    private List<TaskDTO> tasks;
}
