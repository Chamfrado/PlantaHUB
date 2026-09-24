package com.plantahub.api.integration.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Credenciais do Twilio. Preencha {@code fromNumber} ou {@code messagingServiceSid}; se os
 * dois vierem, o Messaging Service ganha.
 */
@ConfigurationProperties(prefix = "app.sms.twilio")
public record TwilioProperties(
        String accountSid,
        String authToken,
        String fromNumber,
        String messagingServiceSid,
        String apiBaseUrl
) {
}
