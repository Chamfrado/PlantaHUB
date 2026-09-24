package com.plantahub.api.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Coloca o {@link InMemoryObjectStorage} na frente do adaptador S3 real.
 *
 * <p>O adaptador de verdade continua sendo criado (construir um {@code S3Client} nao faz
 * chamada de rede), mas nunca e injetado, porque este bean e {@code @Primary}.
 *
 * <p>Um unico bean: como {@link InMemoryObjectStorage} ja implementa
 * {@code ObjectStoragePort}, ele atende tanto quem injeta a porta quanto quem injeta a
 * classe concreta para semear arquivos.
 */
@TestConfiguration
public class TestStorageConfig {

    @Bean
    @Primary
    public InMemoryObjectStorage inMemoryObjectStorage() {
        return new InMemoryObjectStorage();
    }

    /**
     * Bucket configuravel no lugar do adaptador de inspecao real.
     *
     * <p>Comeca no estado correto, entao qualquer teste que nao mexa nele ve um bucket
     * saudavel — que e o cenario certo para quem so precisa que o contexto suba.
     */
    @Bean
    @Primary
    public FakeBucketInspection fakeBucketInspection() {
        return new FakeBucketInspection();
    }
}
