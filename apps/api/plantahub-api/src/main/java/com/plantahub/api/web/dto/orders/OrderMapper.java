package com.plantahub.api.web.dto.orders;

import com.plantahub.api.domain.orders.Order;

public class OrderMapper {
    public static OrderResponseDTO toDto(Order o) {
        return new OrderResponseDTO(
                o.getId(),
                o.getStatus().name(),
                o.getTotalCents(),
                o.getCurrency(),
                o.getCreatedAt(),
                o.getPaidAt(),
                o.getItems().stream().map(i ->
                        new OrderResponseDTO.OrderItemDTO(
                                i.getId(),
                                i.getProduct().getId(),
                                i.getQuantity(),
                                i.getTotalCents(),
                                i.getSelections().stream().map(s ->
                                        new OrderResponseDTO.SelectionDTO(
                                                s.getPlanType().getCode(),
                                                s.getPriceCents()
                                        )
                                ).toList()
                        )
                ).toList()
        );
    }
}