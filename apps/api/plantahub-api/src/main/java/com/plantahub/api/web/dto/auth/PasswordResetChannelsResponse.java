package com.plantahub.api.web.dto.auth;

/** Canais que a tela pode oferecer. O e-mail sempre existe; o SMS depende do provedor. */
public record PasswordResetChannelsResponse(boolean email, boolean sms) {}
