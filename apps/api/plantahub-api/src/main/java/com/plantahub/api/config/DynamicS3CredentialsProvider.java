package com.plantahub.api.config;

import com.plantahub.api.service.admin.S3CredentialsStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

/**
 * Resolve a credencial a cada assinatura, em vez de congelá-la no boot.
 *
 * <p>É isto que permite trocar a chave pelo painel e o efeito valer na requisição seguinte.
 * Sem isso, salvar credenciais exigiria reiniciar a aplicação — e um recurso de painel que
 * só funciona depois de um restart é um recurso que ninguém confia.
 *
 * <p>O {@link S3CredentialsStore} chega por {@link ObjectProvider} porque este provider é
 * construído junto com o cliente da S3, antes de as repositórios JPA estarem
 * necessariamente prontos. Resolver na chamada, e não na construção, evita o ciclo.
 */
public class DynamicS3CredentialsProvider implements AwsCredentialsProvider {

    private static final Logger log = LoggerFactory.getLogger(DynamicS3CredentialsProvider.class);

    private final ObjectProvider<S3CredentialsStore> store;
    private final AwsCredentials configured;
    private final DefaultCredentialsProvider fallback = DefaultCredentialsProvider.create();

    public DynamicS3CredentialsProvider(ObjectProvider<S3CredentialsStore> store,
                                        String accessKey,
                                        String secretKey) {
        this.store = store;

        boolean hasStatic = accessKey != null && !accessKey.isBlank()
                && secretKey != null && !secretKey.isBlank();

        this.configured = hasStatic ? AwsBasicCredentials.create(accessKey, secretKey) : null;
    }

    @Override
    public AwsCredentials resolveCredentials() {
        S3CredentialsStore resolved = store.getIfAvailable();

        if (resolved != null) {
            var fromPanel = resolved.panelCredentials();

            if (fromPanel.isPresent()) {
                return AwsBasicCredentials.create(
                        fromPanel.get().accessKey(), fromPanel.get().secretKey());
            }
        }

        if (configured != null) {
            return configured;
        }

        // Variáveis de ambiente, perfil, instance role. É o caminho preferido em produção:
        // não existe segredo de longa duração para vazar nem para rotacionar.
        log.debug("S3: usando a cadeia padrão de credenciais");
        return fallback.resolveCredentials();
    }
}
