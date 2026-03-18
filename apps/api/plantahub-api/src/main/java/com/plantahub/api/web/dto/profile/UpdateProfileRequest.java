package com.plantahub.api.web.dto.profile;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 200) String fullName,

        @Pattern(
                regexp = "^(\\d{11}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2})$",
                message = "cpf_invalid"
        )
        String cpf,

        @Pattern(
                regexp = "^\\+?[0-9()\\-\\s]{10,20}$",
                message = "phone_invalid"
        )
        String phoneNumber
) {}