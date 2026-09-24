package com.plantahub.api.service.admin;

import com.plantahub.api.domain.config.AppSecret;
import com.plantahub.api.repository.AppSecretRepository;
import com.plantahub.api.shared.exception.ConflictException;
import com.plantahub.api.shared.security.SecretCipher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * As credenciais da S3 que o painel pode definir.
 *
 * <p><b>Precedência: painel &gt; configuração &gt; cadeia padrão do SDK.</b> O painel vence
 * porque a alternativa produz a pior falha possível: alguém cola a chave, salva, e nada
 * muda — sem nenhum sinal de que uma variável de ambiente antiga está no comando. A tela
 * mostra sempre qual fonte está em uso, então a troca nunca é silenciosa.
 *
 * <p>O segredo nunca sai daqui em texto claro para cima: só desce para o provider do SDK.
 * Nem a API nem o log jamais o devolvem.
 */
@Service
public class S3CredentialsStore {

    private static final Logger log = LoggerFactory.getLogger(S3CredentialsStore.class);

    /** Chave pública e segredo, separados por {@code :} dentro do valor cifrado. */
    private static final String SEPARATOR = ":";

    public enum Source {
        /** Gravadas pelo painel, cifradas no banco. */
        PANEL,
        /** {@code app.s3.access-key} / {@code secret-key}. */
        CONFIG,
        /** Nada estático: variáveis de ambiente do processo, perfil ou instance role. */
        DEFAULT_CHAIN
    }

    public record Credentials(String accessKey, String secretKey) {}

    public record Status(
            Source source,
            String accessKeyHint,
            Instant updatedAt,
            String updatedBy
    ) {}

    private final AppSecretRepository repo;
    private final SecretCipher cipher;
    private final String configuredAccessKey;
    private final String configuredSecretKey;

    /**
     * Cache do valor resolvido.
     *
     * <p>O SDK chama o provider a cada requisição assinada; sem cache, todo presign viraria
     * uma consulta ao banco. {@code null} significa "ainda não carregado", e não "ausente".
     */
    private final AtomicReference<Optional<Credentials>> cached = new AtomicReference<>(null);

    public S3CredentialsStore(
            AppSecretRepository repo,
            SecretCipher cipher,
            @Value("${app.s3.access-key:}") String configuredAccessKey,
            @Value("${app.s3.secret-key:}") String configuredSecretKey
    ) {
        this.repo = repo;
        this.cipher = cipher;
        this.configuredAccessKey = configuredAccessKey;
        this.configuredSecretKey = configuredSecretKey;
    }

    /** As credenciais do painel, se houver. */
    public Optional<Credentials> panelCredentials() {
        Optional<Credentials> value = cached.get();

        if (value != null) {
            return value;
        }

        Optional<Credentials> loaded = load();
        cached.set(loaded);
        return loaded;
    }

    private Optional<Credentials> load() {
        try {
            return repo.findById(AppSecret.S3_CREDENTIALS).map(secret -> {
                String plain = cipher.decrypt(secret.getValueEncrypted());
                int split = plain.indexOf(SEPARATOR);

                return new Credentials(plain.substring(0, split), plain.substring(split + 1));
            });
        } catch (RuntimeException e) {
            // Nao pode derrubar o assinador: sem credencial do painel, a cadeia padrao
            // ainda pode funcionar. O diagnostico e a tela certa para expor isto.
            log.error("Credenciais da S3 gravadas no painel nao puderam ser lidas", e);
            return Optional.empty();
        }
    }

    @Transactional(readOnly = true)
    public Status status() {
        var stored = repo.findById(AppSecret.S3_CREDENTIALS);

        if (stored.isPresent() && panelCredentials().isPresent()) {
            return new Status(Source.PANEL, stored.get().getHint(),
                    stored.get().getUpdatedAt(), stored.get().getUpdatedBy());
        }

        boolean hasConfigured = !configuredAccessKey.isBlank() && !configuredSecretKey.isBlank();

        return hasConfigured
                ? new Status(Source.CONFIG, maskOf(configuredAccessKey), null, null)
                : new Status(Source.DEFAULT_CHAIN, null, null, null);
    }

    @Transactional
    public Status save(String accessKey, String secretKey, String updatedBy) {
        String trimmedKey = accessKey == null ? "" : accessKey.trim();
        String trimmedSecret = secretKey == null ? "" : secretKey.trim();

        if (trimmedKey.isBlank() || trimmedSecret.isBlank()) {
            throw new ConflictException("credentials_incomplete");
        }

        if (trimmedKey.contains(SEPARATOR)) {
            // A chave publica da AWS nunca tem ':'; se tiver, o valor esta errado e
            // guardar assim quebraria a leitura de volta de um jeito confuso.
            throw new ConflictException("access_key_invalid");
        }

        String encrypted = cipher.encrypt(trimmedKey + SEPARATOR + trimmedSecret);
        String hint = maskOf(trimmedKey);

        var existing = repo.findById(AppSecret.S3_CREDENTIALS);

        if (existing.isPresent()) {
            existing.get().replace(encrypted, hint, updatedBy);
            repo.save(existing.get());
        } else {
            repo.save(new AppSecret(AppSecret.S3_CREDENTIALS, encrypted, hint, updatedBy));
        }

        cached.set(null);
        log.info("Credenciais da S3 atualizadas pelo painel por {} ({})", updatedBy, hint);

        return status();
    }

    @Transactional
    public Status clear(String updatedBy) {
        repo.deleteById(AppSecret.S3_CREDENTIALS);
        cached.set(null);

        log.info("Credenciais da S3 do painel removidas por {}", updatedBy);
        return status();
    }

    /** Só o suficiente para identificar qual chave está em uso. */
    private static String maskOf(String accessKey) {
        return accessKey.length() <= 4
                ? "••••"
                : "••••" + accessKey.substring(accessKey.length() - 4);
    }
}
