package com.plantahub.api.service;

import com.plantahub.api.config.PasswordResetProperties;
import com.plantahub.api.domain.auth.AppUser;
import com.plantahub.api.domain.auth.PasswordResetCode;
import com.plantahub.api.domain.auth.enums.ResetChannel;
import com.plantahub.api.integration.notification.SmsSender;
import com.plantahub.api.repository.AppUserRepository;
import com.plantahub.api.repository.PasswordResetCodeRepository;
import com.plantahub.api.service.PasswordResetEvents.CodeIssued;
import com.plantahub.api.service.PasswordResetEvents.PasswordChanged;
import com.plantahub.api.shared.exception.TooManyRequestsException;
import com.plantahub.api.shared.security.RequestRateLimiter;
import com.plantahub.api.shared.security.RequestRateLimiter.Rule;
import com.plantahub.api.shared.security.ResetCodeHasher;
import com.plantahub.api.shared.util.PhoneUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Recuperacao de senha em tres passos: pedir o codigo, validar o codigo, definir a senha.
 *
 * <p>O pedido responde igual exista o e-mail ou nao, e o envio acontece fora da requisicao
 * (depois do commit). Assim nem a resposta nem o tempo dela revelam quem tem conta.
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private static final Duration HOUR = Duration.ofHours(1);
    private static final Duration RETENTION = Duration.ofDays(1);

    static final String INVALID_CODE = "invalid_or_expired_code";
    static final String INVALID_TOKEN = "invalid_or_expired_token";

    private final AppUserRepository users;
    private final PasswordResetCodeRepository codes;
    private final ResetCodeHasher hasher;
    private final RequestRateLimiter limiter;
    private final PasswordEncoder encoder;
    private final PasswordResetProperties props;
    private final SmsSender smsSender;
    private final ApplicationEventPublisher events;

    public PasswordResetService(AppUserRepository users,
                                PasswordResetCodeRepository codes,
                                ResetCodeHasher hasher,
                                RequestRateLimiter limiter,
                                PasswordEncoder encoder,
                                PasswordResetProperties props,
                                SmsSender smsSender,
                                ApplicationEventPublisher events) {
        this.users = users;
        this.codes = codes;
        this.hasher = hasher;
        this.limiter = limiter;
        this.encoder = encoder;
        this.props = props;
        this.smsSender = smsSender;
        this.events = events;
    }

    public boolean isSmsAvailable() {
        return smsSender.isAvailable();
    }

    /**
     * Gera e envia um codigo novo, invalidando qualquer pedido anterior do usuario.
     *
     * <p>Os limites por e-mail valem mesmo para e-mails que nao existem: se so os e-mails
     * cadastrados recebessem 429, o limite viraria um jeito de descobrir quem tem conta.
     */
    @Transactional
    public void requestCode(String rawEmail, ResetChannel channel, String ip) {
        String email = rawEmail.trim().toLowerCase();

        if (!limiter.tryAcquire("reset-request-ip:" + ip, props.ipMaxRequestsPerHour(), HOUR)) {
            throw new TooManyRequestsException();
        }
        if (!limiter.tryAcquireAll("reset-request-email:" + email,
                new Rule(1, props.emailCooldown()),
                new Rule(props.emailMaxPerHour(), HOUR))) {
            throw new TooManyRequestsException();
        }
        if (channel == ResetChannel.SMS && !smsSender.isAvailable()) {
            throw new IllegalArgumentException("channel_unavailable");
        }

        AppUser user = users.findByEmailAndActiveTrueAndDeletedAtIsNull(email).orElse(null);
        if (user == null) {
            log.info("Recuperacao de senha pedida para e-mail sem conta ativa");
            return;
        }

        String destination = channel == ResetChannel.EMAIL
                ? user.getEmail()
                : PhoneUtils.toE164(user.getPhoneNumber());
        if (destination == null) {
            log.info("Recuperacao por SMS pedida por usuario {} sem celular cadastrado", user.getId());
            return;
        }

        Instant now = Instant.now();
        Duration ttl = channel == ResetChannel.EMAIL ? props.emailCodeTtl() : props.smsCodeTtl();
        String code = hasher.newCode();

        codes.invalidateOpenForUser(user.getId(), now);
        codes.save(PasswordResetCode.builder()
                .user(user)
                .channel(channel)
                .codeHash(hasher.hash(code))
                .expiresAt(now.plus(ttl))
                .requestIp(ip)
                .createdAt(now)
                .build());

        events.publishEvent(new CodeIssued(channel, destination, user.getFullName(), code, ttl));
    }

    /**
     * Troca um codigo correto por um token de uso unico.
     *
     * <p>{@code noRollbackFor}: a tentativa errada lanca excecao, mas o contador de
     * tentativas precisa ser gravado mesmo assim, senao a forca bruta seria ilimitada.
     */
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public String verifyCode(String rawEmail, String code, String ip) {
        if (!limiter.tryAcquire("reset-verify-ip:" + ip, props.ipMaxVerifyPerHour(), HOUR)) {
            throw new TooManyRequestsException();
        }

        String email = rawEmail.trim().toLowerCase();
        Instant now = Instant.now();

        AppUser user = users.findByEmailAndActiveTrueAndDeletedAtIsNull(email)
                .orElseThrow(() -> new IllegalArgumentException(INVALID_CODE));
        PasswordResetCode pending = codes
                .findFirstByUserIdAndVerifiedAtIsNullAndConsumedAtIsNullAndInvalidatedAtIsNullOrderByCreatedAtDesc(user.getId())
                .filter(c -> c.isCodeUsable(now))
                .orElseThrow(() -> new IllegalArgumentException(INVALID_CODE));

        if (!hasher.matches(code, pending.getCodeHash())) {
            pending.setAttempts(pending.getAttempts() + 1);
            if (pending.getAttempts() >= props.maxAttempts()) {
                pending.setInvalidatedAt(now);
            }
            throw new IllegalArgumentException(INVALID_CODE);
        }

        String token = hasher.newToken();
        pending.setVerifiedAt(now);
        pending.setResetTokenHash(hasher.hash(token));
        pending.setResetTokenExpiresAt(now.plus(props.resetTokenTtl()));
        return token;
    }

    /** Define a senha nova e derruba todas as sessoes abertas do usuario. */
    @Transactional
    public void confirm(String resetToken, String newPassword) {
        Instant now = Instant.now();

        PasswordResetCode reset = codes.findByResetTokenHash(hasher.hash(resetToken))
                .filter(r -> r.isTokenUsable(now))
                .orElseThrow(() -> new IllegalArgumentException(INVALID_TOKEN));

        AppUser user = reset.getUser();
        if (!user.isActive() || user.getDeletedAt() != null) {
            throw new IllegalArgumentException(INVALID_TOKEN);
        }

        user.setPasswordHash(encoder.encode(newPassword));
        user.setPasswordChangedAt(now);
        reset.setConsumedAt(now);
        codes.invalidateOpenForUser(user.getId(), now);

        events.publishEvent(new PasswordChanged(user.getEmail(), user.getFullName()));
    }

    /** Os registros so servem durante a validade; um dia depois nao ha motivo para guarda-los. */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purgeOld() {
        int removed = codes.deleteCreatedBefore(Instant.now().minus(RETENTION));
        if (removed > 0) {
            log.info("Recuperacao de senha: {} registros antigos removidos", removed);
        }
    }
}
