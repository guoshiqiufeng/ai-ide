package com.aiide.service;

import com.aiide.dto.*;
import java.util.List;

public interface PromptService {
    R<List<PromptDTO>> listPrompts();
    R<PromptDTO> getPrompt(Long id);
    R<PromptDTO> updatePrompt(Long id, PromptDTO dto);
    R<PromptDTO> togglePrompt(Long id);
    void initializeDefaultPrompts();
}
