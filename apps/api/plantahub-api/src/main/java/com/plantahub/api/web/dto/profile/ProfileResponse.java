package com.plantahub.api.web.dto.profile;


public record ProfileResponse(
        String email,
        String fullName,
        String cpf,
        String phoneNumber,
        String role
) {}