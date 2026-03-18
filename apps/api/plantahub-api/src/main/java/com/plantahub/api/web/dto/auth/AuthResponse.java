package com.plantahub.api.web.dto.auth;

public record AuthResponse(
        String accessToken,
        String tokenType,
        String fullName
) {}
