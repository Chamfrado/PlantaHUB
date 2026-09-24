package com.plantahub.api.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Coloca o {@link InMemoryNotifications} na frente dos senders reais. Um unico bean atende
 * quem injeta {@code EmailSender} e quem injeta {@code SmsSender}.
 */
@TestConfiguration
public class TestNotificationConfig {

    @Bean
    @Primary
    public InMemoryNotifications inMemoryNotifications() {
        return new InMemoryNotifications();
    }
}
