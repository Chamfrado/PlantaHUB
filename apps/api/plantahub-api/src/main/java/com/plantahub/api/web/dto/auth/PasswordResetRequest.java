package com.plantahub.api.web.dto.auth;

import com.plantahub.api.domain.auth.enums.ResetChannel;
import jakarta.validation.constraints.*;

public record PasswordResetRequest(
        @Email @NotBlank @Size(max = 200) String email,
        @NotNull ResetChannel channel
) {}
