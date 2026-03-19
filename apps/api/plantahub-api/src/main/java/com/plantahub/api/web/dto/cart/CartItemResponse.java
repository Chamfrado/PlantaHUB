package com.plantahub.api.web.dto.cart;

import java.util.List;
import java.util.UUID;

public record CartItemResponse(
        UUID itemId,
        String productId,
        String slug,
        String name,
        String category,
        String shortDescription,
        String heroImageUrl,
        List<CartPlanTypeResponse> selections,
        Integer itemTotalCents
) {}