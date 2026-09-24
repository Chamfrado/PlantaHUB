package com.plantahub.api.shared.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * De onde o navegador busca capa e galeria.
 *
 * <p>Existe como componente para que o upload e o diagnóstico do bucket não cheguem a
 * respostas diferentes: se cada um derivasse a base por conta própria, o diagnóstico
 * poderia aprovar uma URL que o upload nunca grava — ou reprovar a que ele grava.
 *
 * <p>Sem configuração, cai na URL direta do bucket, que é o formato já gravado nas capas
 * semeadas. Configurar {@code app.s3.public-base-url} aponta para um CDN sem tocar em
 * nenhuma linha já existente.
 */
@Component
public class PublicMediaUrls {

    private final String base;
    private final String bucketBase;

    public PublicMediaUrls(
            @Value("${app.s3.bucket}") String bucket,
            @Value("${app.s3.region}") String region,
            @Value("${app.s3.public-base-url:}") String configured
    ) {
        this.bucketBase = trimSlash("https://" + bucket + ".s3." + region + ".amazonaws.com");
        this.base = configured == null || configured.isBlank()
                ? bucketBase
                : trimSlash(configured);
    }

    /** Endereço público de um objeto, pelo caminho que o cliente realmente usa. */
    public String urlFor(String storageKey) {
        return base + "/" + storageKey;
    }

    /**
     * Endereço no próprio endpoint da S3, ignorando o CDN.
     *
     * <p>É o que o diagnóstico usa para checar que os arquivos vendidos <b>não</b> são
     * legíveis: um 403 no CloudFront não prova nada sobre o bucket atrás dele.
     */
    public String directBucketUrlFor(String storageKey) {
        return bucketBase + "/" + storageKey;
    }

    public String base() {
        return base;
    }

    private static String trimSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
