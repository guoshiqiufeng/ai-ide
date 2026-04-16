package com.aiide.controller;

import com.aiide.dto.*;
import com.aiide.service.ApiPresetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/presets")
@RequiredArgsConstructor
public class ApiPresetController {

    private final ApiPresetService apiPresetService;

    @GetMapping
    public R<List<ApiPresetDTO>> listPresets() {
        return apiPresetService.listPresets();
    }

    @GetMapping("/{id}")
    public R<ApiPresetDTO> getPreset(@PathVariable Long id) {
        return apiPresetService.getPreset(id);
    }

    @PostMapping
    public R<ApiPresetDTO> createPreset(@RequestBody ApiPresetDTO dto) {
        return apiPresetService.createPreset(dto);
    }

    @PutMapping("/{id}")
    public R<ApiPresetDTO> updatePreset(@PathVariable Long id, @RequestBody ApiPresetDTO dto) {
        return apiPresetService.updatePreset(id, dto);
    }

    @DeleteMapping("/{id}")
    public R<Void> deletePreset(@PathVariable Long id) {
        return apiPresetService.deletePreset(id);
    }

    @PostMapping("/{id}/activate")
    public R<ApiPresetDTO> activatePreset(@PathVariable Long id) {
        return apiPresetService.activatePreset(id);
    }

    @PostMapping("/{id}/test")
    public R<ApiTestResponse> testPreset(@PathVariable Long id) {
        return apiPresetService.testPreset(id);
    }
}
