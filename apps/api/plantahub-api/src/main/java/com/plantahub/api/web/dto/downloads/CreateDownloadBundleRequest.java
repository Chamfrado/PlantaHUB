package com.plantahub.api.web.dto.downloads;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateDownloadBundleRequest(
        @NotEmpty List<@Valid Item> items
) {
    public record Item(
            @NotBlank String productId,
            @NotEmpty List<@NotBlank String> planTypeCodes
    ) {}
}