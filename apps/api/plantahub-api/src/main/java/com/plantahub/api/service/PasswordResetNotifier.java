package com.plantahub.api.service;

import com.plantahub.api.domain.auth.enums.ResetChannel;
import com.plantahub.api.integration.notification.EmailSender;
import com.plantahub.api.integration.notification.NotificationTemplates;
import com.plantahub.api.integration.notification.SmsSender;
import com.plantahub.api.service.PasswordResetEvents.CodeIssued;
import com.plantahub.api.service.PasswordResetEvents.PasswordChanged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Entrega as mensagens da recuperacao de senha.
 *
 * <p>Depois do commit, para nunca mandar um codigo que o banco nao gravou; e em outra
 * thread, para a latencia do SMTP ou do Twilio nao aparecer no tempo de resposta.
 */
@Component
public class PasswordResetNotifier {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetNotifier.class);

    private final EmailSender emailSender;
    private final SmsSender smsSender;

    public PasswordResetNotifier(EmailSender emailSender, SmsSender smsSender) {
        this.emailSender = emailSender;
        this.smsSender = smsSender;
    }

    @Async("notificationExecutor")
    @TransactionalEventListener
    public void onCodeIssued(CodeIssued event) {
        try {
            if (event.channel() == ResetChannel.SMS) {
                smsSender.send(event.destination(), NotificationTemplates.resetCodeSms(event.code(), event.ttl()));
            } else {
                var email = NotificationTemplates.resetCodeEmail(event.fullName(), event.code(), event.ttl());
                emailSender.send(event.destination(), email.subject(), email.text(), email.html());
            }
        } catch (RuntimeException e) {
            // Sem o destino no log: e dado pessoal e nao ajuda a diagnosticar o provedor.
            log.error("Falha ao enviar codigo de recuperacao por {}", event.channel(), e);
        }
    }

    @Async("notificationExecutor")
    @TransactionalEventListener
    public void onPasswordChanged(PasswordChanged event) {
        try {
            var email = NotificationTemplates.passwordChangedEmail(event.fullName());
            emailSender.send(event.email(), email.subject(), email.text(), email.html());
        } catch (RuntimeException e) {
            log.error("Falha ao enviar aviso de senha alterada", e);
        }
    }
}
