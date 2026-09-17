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
        String paymentUrl,
        List<OrderItemDTO> items
) {
    /**
     * Os campos de snapshot descrevem o produto como ele era no momento da compra.
     * Ler do catalogo atual faria a tela de pedidos mudar quando o admin renomeasse algo.
     */
    public record OrderItemDTO(
            UUID id,
            String productId,
            String productName,
            String productCategory,
            String productSlug,
            String productImageUrl,
            int quantity,
            int totalCents,
            List<SelectionDTO> selections
    ) {}

    public record SelectionDTO(
            String planTypeCode,
            String planTypeName,
            int priceCents
    ) {}
}
