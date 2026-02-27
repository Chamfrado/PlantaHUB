package com.plantahub.api.web.dto.orders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponseDTO(
        UUID id,
        String status,
        int totalCents,
        String currency,
        Instant createdAt,
        Instant paidAt,
        List<OrderItemDTO> items
) {
    public record OrderItemDTO(
            UUID id,
            String productId,
            int quantity,
            int totalCents,
            List<SelectionDTO> selections
    ) {}

    public record SelectionDTO(
            String planTypeCode,
            int priceCents
    ) {}
}