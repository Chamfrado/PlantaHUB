package com.plantahub.api.support;

import org.junit.jupiter.api.Tag;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base para qualquer teste que precise de um Postgres real.
 *
 * <p>Usar Postgres de verdade (e nao H2) e obrigatorio: as migrations usam {@code jsonb},
 * {@code gen_random_uuid()}, indices parciais e expressoes regulares em CHECK.
 *
 * <p>Duas formas de fornecer o banco:
 * <ul>
 *   <li><b>Testcontainers</b> (padrao, usado pela CI). Container singleton estatico,
 *       iniciado uma vez por JVM e reaproveitado por todas as classes de teste — em vez
 *       de {@code @Container}, que levantaria um Postgres por classe.</li>
 *   <li><b>Postgres externo</b>, quando {@code TEST_POSTGRES_URL} estiver definida. Serve
 *       para quem tem Postgres na maquina e nao tem Docker. O banco apontado e migrado
 *       pelo Flyway e usado pelos testes, entao aponte para um banco descartavel.</li>
 * </ul>
 *
 * <p>Para pular estes testes por completo: {@code mvn test -DexcludedGroups=db}.
 */
@Tag("db")
@ActiveProfiles("test")
public abstract class AbstractPostgresTest {

    private static final String EXTERNAL_URL = System.getenv("TEST_POSTGRES_URL");
    private static final String EXTERNAL_USER = System.getenv("TEST_POSTGRES_USER");
    private static final String EXTERNAL_PASSWORD = System.getenv("TEST_POSTGRES_PASSWORD");

    private static final boolean USE_EXTERNAL = EXTERNAL_URL != null && !EXTERNAL_URL.isBlank();

    private static final PostgreSQLContainer<?> POSTGRES = USE_EXTERNAL
            ? null
            : new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("plantahub_test")
                    .withUsername("plantahub")
                    .withPassword("plantahub");

    static {
        if (POSTGRES != null) {
            POSTGRES.start();
        }
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        if (USE_EXTERNAL) {
            registry.add("spring.datasource.url", () -> EXTERNAL_URL);
            registry.add("spring.datasource.username", () -> EXTERNAL_USER);
            registry.add("spring.datasource.password", () -> EXTERNAL_PASSWORD);
        } else {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
        }
    }
}
