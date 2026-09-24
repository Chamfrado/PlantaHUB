package com.plantahub.api.web.dto.auth;

import jakarta.validation.constraints.*;

public record PasswordResetVerifyRequest(
        @Email @NotBlank @Size(max = 200) String email,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "o codigo tem 6 digitos") String code
) {}
