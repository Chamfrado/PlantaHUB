package com.plantahub.api.web.dto.orders;

import java.util.List;
import java.util.UUID;

public record OrderItemDTO(
        UUID id,
        String productId,
        String productName,
        Integer quantity,
        Integer unitPriceCents,
        Integer totalCents,
        List<OrderSelectionDTO> selections
) {}
