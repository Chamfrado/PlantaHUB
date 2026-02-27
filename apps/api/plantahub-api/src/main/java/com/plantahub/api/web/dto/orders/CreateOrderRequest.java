package com.plantahub.api.web.dto.orders;

import jakarta.validation.constraints.*;
import java.util.List;

public record CreateOrderRequest(
        @NotEmpty List<Item> items
) {
    public record Item(
            @NotBlank String productId,
            @Min(1) int quantity,
            @NotEmpty List<@NotBlank String> planTypeCodes // ["ARCH","HYD"...]
    ) {}
}