package com.plantahub.api.shared.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cifra os segredos que o painel grava.
 *
 * <p>AES-GCM: além de cifrar, autentica — um valor adulterado no banco falha ao decifrar
 * em vez de virar lixo silencioso.
 *
 * <p><b>De onde vem a chave.</b> De {@code app.secrets.key} quando configurada; caso
 * contrário, derivada do segredo do JWT com um rótulo próprio. A derivação não é economia
 * de configuração: sem ela, o recurso só funcionaria em quem soubesse criar mais uma
 * variável de ambiente, e a alternativa prática seria guardar a chave da AWS em texto
 * claro. O rótulo separa os domínios, então a chave daqui não serve para assinar token
 * nenhum, nem o contrário.
 *
 * <p>Isto protege contra um cenário concreto e comum: um dump do banco. Não protege contra
 * quem já está dentro do processo — para isso a resposta é não ter segredo de longa duração
 * nenhum, ou seja, usar uma instance role.
 */
@Component
public class SecretCipher {

    private static final String LABEL = "plantahub:app-secret:v1";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public SecretCipher(
            @Value("${app.secrets.key:}") String configuredKey,
            @Value("${security.jwt.secret}") String jwtSecret
    ) {
        String material = configuredKey != null && !configuredKey.isBlank()
                ? configuredKey
                : jwtSecret;

        this.key = new SecretKeySpec(derive(material), "AES");
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));

            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // IV junto do texto cifrado: ele não é secreto, e precisa ser único por
            // mensagem — guardá-lo separado só criaria a chance de perder o par.
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("nao foi possivel cifrar o segredo", e);
        }
    }

    public String decrypt(String ciphertext) {
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertext);

            byte[] iv = new byte[IV_BYTES];
            System.arraycopy(combined, 0, iv, 0, IV_BYTES);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));

            byte[] decrypted = cipher.doFinal(
                    combined, IV_BYTES, combined.length - IV_BYTES);

            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Acontece quando a chave de derivação mudou — trocar o JWT_SECRET, por
            // exemplo. A mensagem precisa dizer isso, senão vira um erro insondável.
            throw new IllegalStateException(
                    "nao foi possivel decifrar o segredo: a chave mudou? "
                            + "Grave as credenciais de novo pelo painel.", e);
        }
    }

    private static byte[] derive(String material) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(LABEL.getBytes(StandardCharsets.UTF_8));
            return digest.digest(material.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponivel", e);
        }
    }
}
