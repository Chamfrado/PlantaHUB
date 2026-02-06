package com.plantahub.api.web.dto.orders;

import jakarta.validation.constraints.*;
import java.util.List;

public record CreateOrderRequest(
        @NotBlank String productId,
        @NotEmpty List<@NotBlank String> selectedPlanTypes
) {}
