package com.plantahub.api.integration.infinitepay.dto;

public record InfinitePayCustomerDTO(
        String name,
        String email,
        String phone_number
) {
}