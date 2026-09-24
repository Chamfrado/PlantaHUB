package com.plantahub.api.migration;

import com.plantahub.api.support.AbstractApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O teste de maior alcance da suite.
 *
 * <p>Sobe o contexto contra um Postgres limpo, o que significa que o Flyway aplica V1 ate
 * a ultima migration e, logo em seguida, o Hibernate roda com {@code ddl-auto=validate}.
 * Qualquer divergencia entre uma {@code @Entity} e o schema real derruba este teste — foi
 * assim que o drift do {@code DigitalAsset} (constraint apontando para a coluna
 * {@code format}, derrubada pela V7) ficou visivel, e e assim que todo drift futuro vai
 * aparecer sem ninguem precisar lembrar de conferir.
 *
 * <p>As checagens que nao dependem de banco vivem em {@link MigrationNamingTest}.
 */
class FlywayMigrationTest extends AbstractApiTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("todas as migrations aplicam com sucesso")
    void migrationsApplyCleanly() {
        List<String> failed = jdbc.queryForList(
                "SELECT script FROM flyway_schema_history WHERE success = false",
                String.class
        );

        assertThat(failed).isEmpty();

        Integer applied = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success = true",
                Integer.class
        );

        assertThat(applied).isNotNull().isGreaterThan(0);
    }

    @Test
    @DisplayName("a versao 12 nao esta no historico do Flyway")
    void versionTwelveIsAbsent() {
        List<String> versions = jdbc.queryForList(
                "SELECT version FROM flyway_schema_history WHERE version IS NOT NULL",
                String.class
        );

        assertThat(versions).doesNotContain("12");
    }
}
