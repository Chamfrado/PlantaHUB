package com.plantahub.api.integration.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/** Envia pela API REST do Twilio. Sem SDK: e um unico POST de formulario. */
@Component
@ConditionalOnProperty(prefix = "app.sms", name = "enabled", havingValue = "true")
public class TwilioSmsSender implements SmsSender {

    private final RestClient restClient;
    private final TwilioProperties properties;

    public TwilioSmsSender(TwilioProperties properties) {
        if (isBlank(properties.accountSid()) || isBlank(properties.authToken())) {
            throw new IllegalStateException("app.sms.twilio.account-sid e auth-token sao obrigatorios com SMS ligado");
        }
        if (isBlank(properties.fromNumber()) && isBlank(properties.messagingServiceSid())) {
            throw new IllegalStateException("informe app.sms.twilio.from-number ou messaging-service-sid");
        }
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(isBlank(properties.apiBaseUrl()) ? "https://api.twilio.com" : properties.apiBaseUrl())
                .defaultHeaders(h -> h.setBasicAuth(properties.accountSid(), properties.authToken()))
                .build();
    }

    @Override
    public void send(String toE164, String body) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", toE164);
        form.add("Body", body);
        if (!isBlank(properties.messagingServiceSid())) {
            form.add("MessagingServiceSid", properties.messagingServiceSid());
        } else {
            form.add("From", properties.fromNumber());
        }

        restClient.post()
                .uri("/2010-04-01/Accounts/{sid}/Messages.json", properties.accountSid())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
