package com.plantahub.api.web.dto.catalog;

public record ProductDetailDTO(
        String id,
        String category,
        String slug,
        String name,
        String shortDescription,
        Integer areaM2,
        String heroImageUrl,
        String delivery,
        Boolean customizable
) {}
