package com.plantahub.api.web.dto.catalog;

public record PlanTypeOptionDTO(
        String code,
        String name,
        String description,
        Integer priceCents,
        Boolean includedInBundle
) {}
