package com.plantahub.api.web.dto.cart;

import java.util.UUID;

public record CartPlanTypeResponse(
        UUID id,
        String code,
        String name,
        String description,
        Integer priceCents
) {}