package com.plantahub.api.integration.notification;

/** Envio de SMS. */
public interface SmsSender {

    /** @param toE164 numero no formato E.164, por exemplo {@code +5511999998888} */
    void send(String toE164, String body);

    /** Falso quando nao ha provedor configurado: a tela nem oferece a opcao. */
    boolean isAvailable();
}
