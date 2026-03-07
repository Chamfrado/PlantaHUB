package com.plantahub.api.web.dto.orders;


import java.util.UUID;

public record OrderActionResponseDTO(
        UUID orderId,
        String status,
        String message
) {}