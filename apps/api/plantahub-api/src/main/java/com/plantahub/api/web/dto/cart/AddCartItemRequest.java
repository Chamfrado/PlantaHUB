package com.plantahub.api.web.dto.cart;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AddCartItemRequest(
        @NotBlank String productId,
        @NotEmpty List<@NotBlank String> planTypeCodes
) {}