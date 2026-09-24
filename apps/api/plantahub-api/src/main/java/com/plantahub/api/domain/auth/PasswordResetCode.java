package com.plantahub.api.domain.auth;

import com.plantahub.api.domain.auth.enums.ResetChannel;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Um pedido de recuperacao de senha.
 *
 * <p>Passa por duas fases: primeiro vale o codigo de 6 digitos enviado ao usuario; depois
 * de validado, o codigo morre e passa a valer um token de uso unico que autoriza a troca.
 * Os dois ficam guardados apenas como HMAC.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "password_reset_code")
public class PasswordResetCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 10)
    private ResetChannel channel;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "attempts", nullable = false)
    @Builder.Default
    private int attempts = 0;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "reset_token_hash", length = 64)
    private String resetTokenHash;

    @Column(name = "reset_token_expires_at")
    private Instant resetTokenExpiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "invalidated_at")
    private Instant invalidatedAt;

    @Column(name = "request_ip", length = 45)
    private String requestIp;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** O codigo ainda pode ser tentado. */
    public boolean isCodeUsable(Instant now) {
        return verifiedAt == null && consumedAt == null && invalidatedAt == null && now.isBefore(expiresAt);
    }

    /** O token ainda autoriza a troca de senha. */
    public boolean isTokenUsable(Instant now) {
        return verifiedAt != null && consumedAt == null && invalidatedAt == null
                && resetTokenExpiresAt != null && now.isBefore(resetTokenExpiresAt);
    }
}
