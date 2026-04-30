package com.plantahub.api.web.dto.checkout;

import com.plantahub.api.web.dto.orders.OrderResponseDTO;

public record CheckoutFromCartResponseDTO(
        OrderResponseDTO order,
        String paymentUrl
) {
}