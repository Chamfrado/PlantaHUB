package com.plantahub.api.integration.notification;

/** Envio de e-mail transacional. */
public interface EmailSender {

    /**
     * @param text versao em texto puro, para clientes que nao renderizam HTML
     * @param html versao principal
     */
    void send(String to, String subject, String text, String html);
}
