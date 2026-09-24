package com.plantahub.api.web.dto.catalog;

import java.util.List;

public record ProductSummaryDTO(
        String id,
        String category,
        String categoryName,
        String slug,
        String name,
        String shortDescription,
        Integer areaM2,
        String heroImageUrl,
        Boolean customizable,
        Integer basePriceCents,
        List<String> tags,
        List<String> fileFormats
) {}
