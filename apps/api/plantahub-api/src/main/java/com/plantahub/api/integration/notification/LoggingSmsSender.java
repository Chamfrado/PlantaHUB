package com.plantahub.api.integration.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Usado quando o SMS esta desligado. Em desenvolvimento a opcao aparece e a mensagem vai
 * para o log; em producao a opcao some da tela, ja que nada seria entregue.
 */
@Component
@ConditionalOnProperty(prefix = "app.sms", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LoggingSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsSender.class);

    private final boolean prod;

    public LoggingSmsSender(Environment environment) {
        this.prod = environment.matchesProfiles("prod");
    }

    @Override
    public void send(String toE164, String body) {
        if (prod) {
            log.warn("SMS desligado (app.sms.enabled=false): mensagem descartada");
            return;
        }
        log.info("[SMS de desenvolvimento] para={}\n{}", toE164, body);
    }

    @Override
    public boolean isAvailable() {
        return !prod;
    }
}
