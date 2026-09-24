package com.plantahub.api.service;

import com.plantahub.api.domain.auth.enums.ResetChannel;

import java.time.Duration;

/** Eventos da recuperacao de senha, entregues ao {@link PasswordResetNotifier} depois do commit. */
public final class PasswordResetEvents {

    private PasswordResetEvents() {
    }

    /** O {@code code} so existe em memoria: no banco fica apenas o HMAC dele. */
    public record CodeIssued(ResetChannel channel, String destination, String fullName, String code, Duration ttl) {

        @Override
        public String toString() {
            return "CodeIssued[channel=" + channel + "]";
        }
    }

    public record PasswordChanged(String email, String fullName) {}
}
