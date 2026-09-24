package com.plantahub.api.web.dto.auth;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @Size(max = 200) String fullName,
        // Opcional: so serve para a recuperacao de senha por SMS.
        @Size(max = 20) String phoneNumber
) {}
