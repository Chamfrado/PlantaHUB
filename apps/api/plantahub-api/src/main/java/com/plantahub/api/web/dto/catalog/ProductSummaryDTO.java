package com.plantahub.api.web.dto.catalog;

public record ProductSummaryDTO(
        String id,
        String category,
        String slug,
        String name,
        String shortDescription,
        Integer areaM2,
        String heroImageUrl,
        Boolean customizable,
        Integer basePriceCents
) {}
