package com.plantahub.api.web.dto.cart;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ReplaceCartItemSelectionsRequest(
        @NotEmpty List<@NotNull UUID> planTypeIds
) {}