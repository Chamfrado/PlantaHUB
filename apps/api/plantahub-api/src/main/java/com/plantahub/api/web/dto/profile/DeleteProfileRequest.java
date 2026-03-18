package com.plantahub.api.web.dto.profile;

import jakarta.validation.constraints.NotBlank;

public record DeleteProfileRequest(
        @NotBlank String confirmationText
) {}