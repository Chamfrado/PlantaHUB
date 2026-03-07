package com.plantahub.api.web.dto.Library;

import java.time.Instant;
import java.util.List;

public record LibraryProductDTO(
        String productId,
        String category,
        String slug,
        String name,
        String heroImageUrl,
        Integer areaM2,
        Instant purchasedAt,
        List<LibraryPlanTypeDTO> planTypes
) {}
