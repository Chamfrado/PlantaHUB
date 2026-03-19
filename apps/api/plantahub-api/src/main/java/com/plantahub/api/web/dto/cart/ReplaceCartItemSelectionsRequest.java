package com.plantahub.api.web.dto.cart;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReplaceCartItemSelectionsRequest(
        @NotEmpty List<@NotBlank String> planTypeCodes
) {}