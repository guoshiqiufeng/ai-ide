package com.aiide.service;

import com.aiide.dto.*;
import java.util.List;

public interface ApiPresetService {
    R<List<ApiPresetDTO>> listPresets();
    R<ApiPresetDTO> getPreset(Long id);
    R<ApiPresetDTO> createPreset(ApiPresetDTO dto);
    R<ApiPresetDTO> updatePreset(Long id, ApiPresetDTO dto);
    R<Void> deletePreset(Long id);
    R<ApiPresetDTO> activatePreset(Long id);
    R<ApiTestResponse> testPreset(Long id);
}
