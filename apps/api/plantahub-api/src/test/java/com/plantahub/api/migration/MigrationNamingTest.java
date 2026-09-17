package com.plantahub.api.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checagens sobre os arquivos de migration. Sem Spring, sem Postgres, sem Docker —
 * de proposito: sao as regras que mais precisam rodar em qualquer maquina, a qualquer
 * momento, e nenhuma delas depende do estado de um banco.
 */
class MigrationNamingTest {

    private static final Pattern VALID_MIGRATION_NAME = Pattern.compile("^V\\d+__.+\\.sql$");

    @Test
    @DisplayName("todo arquivo de migration segue o padrao V<n>__<descricao>.sql")
    void migrationFilesFollowNamingConvention() {
        List<String> invalid = migrationFilenames().stream()
                .filter(name -> !VALID_MIGRATION_NAME.matcher(name).matches())
                .toList();

        assertThat(invalid)
                .as("o Flyway ignora em silencio arquivos fora do padrao: "
                        + "validateMigrationNaming e false por default, entao uma migration "
                        + "mal nomeada nunca e aplicada e ninguem percebe")
                .isEmpty();
    }

    @Test
    @DisplayName("nao ha duas migrations com o mesmo numero de versao")
    void migrationVersionsAreUnique() {
        List<String> versions = migrationFilenames().stream()
                .map(name -> name.substring(1, name.indexOf("__")))
                .toList();

        assertThat(versions).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("a versao 12 nao pode ser reintroduzida")
    void versionTwelveMustNotComeBack() {
        // Existia um V12_correct_region.sql com um underscore so. O Flyway o ignorava,
        // entao ele nunca entrou no flyway_schema_history de nenhum banco, e seu efeito
        // ja tinha sido obtido pela V17. Renomea-lo para V12__ faria o Flyway tentar
        // aplicar a versao 12 depois da V19 -> falha out-of-order em toda base existente.
        // Por isso o arquivo foi deletado, e por isso este teste existe.
        List<String> twelve = migrationFilenames().stream()
                .filter(name -> name.startsWith("V12"))
                .toList();

        assertThat(twelve)
                .as("a versao 12 foi pulada deliberadamente; veja o comentario neste teste")
                .isEmpty();
    }

    private List<String> migrationFilenames() {
        try {
            Path dir = Paths.get(
                    getClass().getClassLoader().getResource("db/migration").toURI()
            );

            try (Stream<Path> files = Files.list(dir)) {
                return files
                        .filter(Files::isRegularFile)
                        .map(p -> p.getFileName().toString())
                        .filter(name -> name.endsWith(".sql"))
                        .sorted()
                        .toList();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
