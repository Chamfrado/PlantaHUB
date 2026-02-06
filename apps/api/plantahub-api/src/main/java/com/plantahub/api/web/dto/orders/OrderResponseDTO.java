package com.plantahub.api.web.dto.orders;

import java.util.List;
import java.util.UUID;

public record OrderResponseDTO(
        UUID orderId,
        String status,
        String currency,
        Integer totalCents,
        List<OrderItemDTO> items
) {}
