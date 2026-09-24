package com.plantahub.api.shared.security;

import com.plantahub.api.shared.security.RequestRateLimiter.Rule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class RequestRateLimiterTest {

    /** Relogio que o teste avanca na mao. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private final MutableClock clock = new MutableClock();
    private final RequestRateLimiter limiter = new RequestRateLimiter(clock);

    @Test
    @DisplayName("libera ate o limite e volta a liberar quando a janela passa")
    void slidingWindow() {
        assertThat(limiter.tryAcquire("k", 2, Duration.ofMinutes(1))).isTrue();
        assertThat(limiter.tryAcquire("k", 2, Duration.ofMinutes(1))).isTrue();
        assertThat(limiter.tryAcquire("k", 2, Duration.ofMinutes(1))).isFalse();

        clock.advance(Duration.ofSeconds(61));
        assertThat(limiter.tryAcquire("k", 2, Duration.ofMinutes(1))).isTrue();
    }

    @Test
    @DisplayName("chaves diferentes nao dividem o limite")
    void keysAreIndependent() {
        assertThat(limiter.tryAcquire("a", 1, Duration.ofMinutes(1))).isTrue();
        assertThat(limiter.tryAcquire("b", 1, Duration.ofMinutes(1))).isTrue();
        assertThat(limiter.tryAcquire("a", 1, Duration.ofMinutes(1))).isFalse();
    }

    @Test
    @DisplayName("com varias regras, a mais restritiva decide e a recusa nao conta")
    void multipleRules() {
        Rule cooldown = new Rule(1, Duration.ofSeconds(60));
        Rule hourly = new Rule(2, Duration.ofHours(1));

        assertThat(limiter.tryAcquireAll("e", cooldown, hourly)).isTrue();
        assertThat(limiter.tryAcquireAll("e", cooldown, hourly)).isFalse();

        clock.advance(Duration.ofSeconds(61));
        assertThat(limiter.tryAcquireAll("e", cooldown, hourly)).isTrue();

        clock.advance(Duration.ofSeconds(61));
        assertThat(limiter.tryAcquireAll("e", cooldown, hourly)).as("limite por hora").isFalse();

        clock.advance(Duration.ofHours(1));
        assertThat(limiter.tryAcquireAll("e", cooldown, hourly)).isTrue();
    }

    @Test
    @DisplayName("a limpeza remove chaves sem eventos recentes")
    void evictionKeepsWorking() {
        limiter.tryAcquire("velha", 1, Duration.ofMinutes(1));
        clock.advance(Duration.ofHours(2));
        limiter.evictExpired();

        assertThat(limiter.tryAcquire("velha", 1, Duration.ofMinutes(1))).isTrue();
    }
}
