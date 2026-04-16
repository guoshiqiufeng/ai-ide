package com.aiide.controller;

import com.aiide.dto.*;
import com.aiide.service.PromptService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prompts")
@RequiredArgsConstructor
public class PromptController {

    private final PromptService promptService;

    @GetMapping
    public R<List<PromptDTO>> listPrompts() {
        return promptService.listPrompts();
    }

    @GetMapping("/{id}")
    public R<PromptDTO> getPrompt(@PathVariable Long id) {
        return promptService.getPrompt(id);
    }

    @PutMapping("/{id}")
    public R<PromptDTO> updatePrompt(@PathVariable Long id, @RequestBody PromptDTO dto) {
        return promptService.updatePrompt(id, dto);
    }

    @PostMapping("/{id}/toggle")
    public R<PromptDTO> togglePrompt(@PathVariable Long id) {
        return promptService.togglePrompt(id);
    }
}
