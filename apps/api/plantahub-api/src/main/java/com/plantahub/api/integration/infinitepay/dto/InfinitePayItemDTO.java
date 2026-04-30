package com.plantahub.api.integration.infinitepay.dto;

public record InfinitePayItemDTO(
        Integer quantity,
        Long price,
        String description
) {
}