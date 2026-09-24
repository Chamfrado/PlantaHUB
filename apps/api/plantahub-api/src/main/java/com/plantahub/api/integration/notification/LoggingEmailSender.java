package com.plantahub.api.integration.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Usado quando o SMTP esta desligado: em desenvolvimento, escreve a mensagem no log para
 * quem esta testando ler o codigo. Em producao o corpo nunca vai para o log, porque ali
 * ele equivaleria a uma senha em texto puro.
 */
@Component
@ConditionalOnProperty(prefix = "app.mail", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    private final boolean prod;

    public LoggingEmailSender(Environment environment) {
        this.prod = environment.matchesProfiles("prod");
    }

    @Override
    public void send(String to, String subject, String text, String html) {
        if (prod) {
            log.warn("E-mail desligado (app.mail.enabled=false): mensagem '{}' descartada", subject);
            return;
        }
        log.info("[e-mail de desenvolvimento] para={} assunto={}\n{}", to, subject, text);
    }
}
