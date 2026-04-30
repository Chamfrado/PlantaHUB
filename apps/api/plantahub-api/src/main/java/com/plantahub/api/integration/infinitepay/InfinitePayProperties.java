package com.plantahub.api.integration.infinitepay;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "infinitepay")
public record InfinitePayProperties(
        String handle,
        String redirectUrl,
        String webhookUrl,
        String apiBaseUrl
) {
}