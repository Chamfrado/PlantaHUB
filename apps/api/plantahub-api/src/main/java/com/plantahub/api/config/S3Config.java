package com.plantahub.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.plantahub.api.service.admin.S3CredentialsStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

    private static final Logger log = LoggerFactory.getLogger(S3Config.class);

    private final String region;
    private final String accessKey;
    private final String secretKey;
    private final ObjectProvider<S3CredentialsStore> credentialsStore;

    public S3Config(
            @Value("${app.s3.region}") String region,
            @Value("${app.s3.access-key:}") String accessKey,
            @Value("${app.s3.secret-key:}") String secretKey,
            ObjectProvider<S3CredentialsStore> credentialsStore
    ) {
        this.region = region;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.credentialsStore = credentialsStore;
    }

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider())
                .build();
    }

    /**
     * Resolve a credencial <b>a cada assinatura</b>, na ordem painel → configuração →
     * cadeia padrão do SDK.
     *
     * <p>Antes isto era decidido uma vez, no boot. Passar a decidir por chamada é o que
     * faz a credencial gravada pelo painel valer sem reiniciar a aplicação.
     *
     * <p>Deixar tudo vazio e usar uma instance role continua sendo o caminho recomendado em
     * produção: é o único em que não existe segredo de longa duração para vazar nem para
     * rotacionar.
     */
    private AwsCredentialsProvider credentialsProvider() {
        log.info("S3: credenciais resolvidas dinamicamente (painel > configuração > cadeia padrão)");
        return new DynamicS3CredentialsProvider(credentialsStore, accessKey, secretKey);
    }
}
