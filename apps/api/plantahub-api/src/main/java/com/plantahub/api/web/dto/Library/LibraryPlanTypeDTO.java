package com.plantahub.api.web.dto.Library;

import java.util.List;

public record LibraryPlanTypeDTO(
        String code,
        String name,
        List<LibraryAssetDTO> assets
) {}
