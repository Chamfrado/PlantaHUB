package com.plantahub.api.web.dto.cart;

import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID cartId,
        String status,
        List<CartItemResponse> items,
        Integer totalCents,
        String currency
) {}