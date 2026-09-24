package com.plantahub.api.web.dto.orders;

import com.plantahub.api.domain.orders.Order;
import com.plantahub.api.domain.orders.OrderItem;
import com.plantahub.api.domain.orders.OrderItemSelection;

public class OrderMapper {

    public static OrderResponseDTO toDto(Order o) {
        return new OrderResponseDTO(
                o.getId(),
                o.getStatus().name(),
                o.getTotalCents(),
                o.getCurrency(),
                o.getCreatedAt(),
                o.getPaidAt(),
                o.getPaymentUrl(),
                o.getItems().stream().map(OrderMapper::toItem).toList()
        );
    }

    private static OrderResponseDTO.OrderItemDTO toItem(OrderItem item) {
        return new OrderResponseDTO.OrderItemDTO(
                item.getId(),
                item.getProduct().getId(),
                // Prefere o snapshot; cai no catalogo vivo apenas para pedidos anteriores
                // a existencia da coluna, cujo backfill pode nao ter alcancado tudo.
                fallback(item.getProductNameSnapshot(), item.getProduct().getName()),
                fallback(item.getProductCategorySnapshot(), item.getProduct().getCategory()),
                fallback(item.getProductSlugSnapshot(), item.getProduct().getSlug()),
                fallback(item.getProductImageSnapshot(), item.getProduct().getHeroImageUrl()),
                item.getQuantity(),
                item.getTotalCents(),
                item.getSelections().stream().map(OrderMapper::toSelection).toList()
        );
    }

    private static OrderResponseDTO.SelectionDTO toSelection(OrderItemSelection selection) {
        return new OrderResponseDTO.SelectionDTO(
                fallback(selection.getPlanTypeCodeSnapshot(), selection.getPlanType().getCode()),
                fallback(selection.getPlanTypeNameSnapshot(), selection.getPlanType().getName()),
                selection.getPriceCents()
        );
    }

    private static String fallback(String snapshot, String live) {
        return snapshot != null && !snapshot.isBlank() ? snapshot : live;
    }
}
