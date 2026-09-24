package com.plantahub.api.support;

import com.plantahub.api.integration.notification.EmailSender;
import com.plantahub.api.integration.notification.SmsSender;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * E-mail e SMS em memoria: guarda tudo o que seria enviado, para o teste ler o codigo.
 *
 * <p>O envio real e assincrono, entao a leitura espera a mensagem chegar em vez de
 * assumir que ela ja esta la.
 */
public class InMemoryNotifications implements EmailSender, SmsSender {

    public record Message(String to, String subject, String text) {}

    private static final Pattern CODE = Pattern.compile("\\b(\\d{6})\\b");

    private final List<Message> messages = new CopyOnWriteArrayList<>();

    @Override
    public void send(String to, String subject, String text, String html) {
        messages.add(new Message(to, subject, text));
    }

    @Override
    public void send(String toE164, String body) {
        messages.add(new Message(toE164, null, body));
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    public List<Message> to(String destination) {
        return messages.stream().filter(m -> m.to().equals(destination)).toList();
    }

    /** Espera a mensagem que satisfaz o filtro, por ate 5 segundos. */
    public Message await(String destination, java.util.function.Predicate<Message> filter) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
        while (Instant.now().isBefore(deadline)) {
            Optional<Message> found = to(destination).stream().filter(filter).reduce((a, b) -> b);
            if (found.isPresent()) {
                return found.get();
            }
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
        throw new AssertionError("nenhuma mensagem esperada chegou para " + destination);
    }

    /** O codigo da mensagem mais recente de recuperacao para o destino. */
    public String awaitCode(String destination, int alreadySeen) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
        while (Instant.now().isBefore(deadline)) {
            List<Message> received = to(destination);
            if (received.size() > alreadySeen) {
                Matcher m = CODE.matcher(received.get(received.size() - 1).text());
                if (m.find()) {
                    return m.group(1);
                }
            }
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
        throw new AssertionError("nenhum codigo chegou para " + destination);
    }
}
