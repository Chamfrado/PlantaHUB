package com.plantahub.api.shared.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Janela deslizante em memoria, por chave ({@code "ip:1.2.3.4"}, {@code "email:x@y"}).
 *
 * <p>Em memoria porque a API roda numa unica VM: um reinicio zera os contadores, o que
 * e aceitavel para um freio contra abuso. Com mais de uma instancia, isto teria de ir
 * para o banco ou para um cache compartilhado.
 */
@Component
public class RequestRateLimiter {

    private static final Duration MAX_WINDOW = Duration.ofHours(1);

    private final Map<String, Deque<Long>> events = new ConcurrentHashMap<>();
    private final Clock clock;

    public RequestRateLimiter() {
        this(Clock.systemUTC());
    }

    RequestRateLimiter(Clock clock) {
        this.clock = clock;
    }

    /**
     * Registra um evento se a chave ainda estiver dentro do limite.
     *
     * @return {@code false} se o limite ja foi atingido; nesse caso nada e registrado
     */
    public boolean tryAcquire(String key, int maxEvents, Duration window) {
        return tryAcquireAll(key, new Rule(maxEvents, window));
    }

    /** Verifica varias regras de uma vez: so registra se todas passarem. */
    public boolean tryAcquireAll(String key, Rule... rules) {
        long now = clock.millis();
        Deque<Long> deque = events.computeIfAbsent(key, k -> new ArrayDeque<>());

        synchronized (deque) {
            for (Rule rule : rules) {
                long since = now - rule.window().toMillis();
                long inWindow = deque.stream().filter(t -> t > since).count();
                if (inWindow >= rule.maxEvents()) {
                    return false;
                }
            }
            deque.addLast(now);
            return true;
        }
    }

    public record Rule(int maxEvents, Duration window) {}

    /** Descarta eventos mais velhos que a maior janela usada, para o mapa nao crescer sem fim. */
    @Scheduled(fixedDelay = 600_000)
    public void evictExpired() {
        long cutoff = clock.millis() - MAX_WINDOW.toMillis();
        events.entrySet().removeIf(entry -> {
            Deque<Long> deque = entry.getValue();
            synchronized (deque) {
                while (!deque.isEmpty() && deque.peekFirst() <= cutoff) {
                    deque.pollFirst();
                }
                return deque.isEmpty();
            }
        });
    }
}
