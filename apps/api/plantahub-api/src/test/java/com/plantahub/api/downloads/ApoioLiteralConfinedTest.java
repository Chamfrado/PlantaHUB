package com.plantahub.api.downloads;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O objetivo central desta modernizacao: o sistema nao pode conhecer o nome de nenhuma
 * colecao de antemao, nem montar caminho de arquivo por convencao.
 *
 * <p>{@code APOIO} era um literal repetido em tres servicos, e {@code "products/" + id + ...}
 * era como o sistema descobria o que entregar — a origem dos {@code NoSuchKey} quando a
 * convencao assumida pelo codigo divergia do bucket. Hoje ambos sobrevivem apenas dentro do
 * ramo de compatibilidade com compras anteriores a pinagem.
 *
 * <p>Quando {@code app.downloads.legacy-fallback} for desligada em producao e o ramo for
 * removido, as listas esperadas aqui passam a ser vazias — este teste e o lembrete.
 */
class ApoioLiteralConfinedTest {

    private static final Path SERVICE_DIR =
            Paths.get("src/main/java/com/plantahub/api/service");

    /** Metodos que existem exatamente para preservar o comportamento antigo. */
    private static final Set<String> LEGACY_FILES = Set.of(
            "DownloadService.java",
            "LibraryService.java",
            "DownloadBundleService.java"
    );

    private static final Pattern METHOD_DECLARATION =
            Pattern.compile("^\\s+(?:private|public|protected)\\s+[\\w<>,\\[\\]. ]+\\s+(\\w+)\\s*\\(");

    private record Occurrence(String file, int line, String method, String text) {}

    private List<Occurrence> findOccurrences(String needle) {
        try (Stream<Path> files = Files.walk(SERVICE_DIR)) {
            List<Occurrence> found = new ArrayList<>();

            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                List<String> lines = Files.readAllLines(file);
                String currentMethod = "<classe>";

                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);

                    Matcher matcher = METHOD_DECLARATION.matcher(line);
                    if (matcher.find()) {
                        currentMethod = matcher.group(1);
                    }

                    // Comentarios explicam o historico; o que importa e o codigo.
                    String trimmed = line.trim();
                    if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) {
                        continue;
                    }

                    if (line.contains(needle)) {
                        found.add(new Occurrence(
                                file.getFileName().toString(), i + 1, currentMethod, trimmed));
                    }
                }
            }

            return found;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    @DisplayName("o literal APOIO so existe dentro do ramo de compatibilidade")
    void apoioLiteralOnlyInLegacyBranch() {
        List<Occurrence> occurrences = findOccurrences("APOIO");

        Map<String, String> offenders = new LinkedHashMap<>();

        for (Occurrence occurrence : occurrences) {
            boolean allowed = LEGACY_FILES.contains(occurrence.file())
                    && occurrence.method().toLowerCase(Locale.ROOT).contains("legacy");

            if (!allowed) {
                offenders.put(occurrence.file() + ":" + occurrence.line()
                        + " (" + occurrence.method() + ")", occurrence.text());
            }
        }

        assertThat(offenders)
                .as("nenhum caminho vivo pode conhecer o nome de uma colecao: "
                        + "colecoes sao criadas pelo admin e o sistema nao pode presumir nenhuma")
                .isEmpty();
    }

    @Test
    @DisplayName("chave de arquivo nunca e montada por convencao fora do ramo de compatibilidade")
    void storageKeysAreNeverRebuilt() {
        List<Occurrence> occurrences = findOccurrences("\"products/\" +");

        Map<String, String> offenders = new LinkedHashMap<>();

        for (Occurrence occurrence : occurrences) {
            boolean allowed = LEGACY_FILES.contains(occurrence.file())
                    && occurrence.method().toLowerCase(Locale.ROOT).contains("legacy");

            if (!allowed) {
                offenders.put(occurrence.file() + ":" + occurrence.line()
                        + " (" + occurrence.method() + ")", occurrence.text());
            }
        }

        assertThat(offenders)
                .as("a chave de leitura vem sempre de digital_asset.storage_key; "
                        + "monta-la por convencao e o que produzia NoSuchKey")
                .isEmpty();
    }

    @Test
    @DisplayName("o ramo de compatibilidade ainda existe e esta contido")
    void legacyBranchIsStillPresentAndContained() {
        List<Occurrence> apoio = findOccurrences("APOIO");

        // Enquanto houver compras sem arquivos pinados, o ramo precisa existir. Se esta
        // lista ficar vazia, a flag ja pode ser desligada e este teste, simplificado.
        assertThat(apoio)
                .as("se o ramo de compatibilidade foi removido, simplifique este teste "
                        + "para exigir zero ocorrencias")
                .isNotEmpty();

        assertThat(apoio)
                .extracting(Occurrence::file)
                .containsOnly("DownloadService.java", "LibraryService.java", "DownloadBundleService.java");
    }
}
