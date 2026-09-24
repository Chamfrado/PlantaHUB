package com.plantahub.api.web.dto.auth;

import jakarta.validation.constraints.*;

public record PasswordResetConfirmRequest(
        @NotBlank @Size(max = 100) String resetToken,
        @NotBlank @Size(min = 8, max = 72) String newPassword
) {}
