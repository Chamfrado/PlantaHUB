package com.plantahub.api.shared.security;

import com.plantahub.api.config.PasswordResetProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Gera e protege os segredos da recuperacao de senha.
 *
 * <p>HMAC em vez de BCrypt: um codigo de 6 digitos tem so um milhao de valores, e nenhum
 * custo de hash impediria forca bruta offline. O que protege o codigo e a chave, que fica
 * fora do banco. Ai um SHA-256 com chave basta e deixa a busca por token ser um indice.
 */
@Component
public class ResetCodeHasher {

    private static final String ALGORITHM = "HmacSHA256";

    private final SecureRandom random = new SecureRandom();
    private final SecretKeySpec key;

    public ResetCodeHasher(PasswordResetProperties properties) {
        if (properties.secret() == null || properties.secret().length() < 32) {
            throw new IllegalStateException("app.password-reset.secret precisa ter pelo menos 32 caracteres");
        }
        this.key = new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    /** Seis digitos, com zeros a esquerda. */
    public String newCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    /** 32 bytes aleatorios em base64url, prontos para trafegar em JSON. */
    public String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hash(String value) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC indisponivel", e);
        }
    }

    /** Comparacao em tempo constante, para o tempo de resposta nao vazar quantos digitos batem. */
    public boolean matches(String value, String expectedHash) {
        if (value == null || expectedHash == null) return false;
        return MessageDigest.isEqual(
                hash(value).getBytes(StandardCharsets.US_ASCII),
                expectedHash.getBytes(StandardCharsets.US_ASCII));
    }
}
