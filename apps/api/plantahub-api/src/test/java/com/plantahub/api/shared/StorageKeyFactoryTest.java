package com.plantahub.api.shared;

import com.plantahub.api.shared.storage.StorageKeyFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Os nomes reais sao em portugues: acento, espaco, parentese e til aparecem o tempo todo.
 * Eles sobrevivem numa chave S3, mas exigem escape em URL e viram fonte constante de
 * confusao — por isso a chave usa a versao normalizada, e o nome original fica no banco
 * para exibicao.
 */
class StorageKeyFactoryTest {

    private final StorageKeyFactory factory = new StorageKeyFactory();

    @Test
    @DisplayName("remove acentos, espacos e parenteses do nome do arquivo")
    void normalizesPortugueseFilenames() {
        assertThat(factory.normalizeFilename("Planta Arquitetônica (final) v2.PDF"))
                .isEqualTo("planta-arquitetonica-final-v2.pdf");

        assertThat(factory.normalizeFilename("Memorial Descritivo — Revisão 3.docx"))
                .isEqualTo("memorial-descritivo-revisao-3.docx");
    }

    @Test
    @DisplayName("nome vazio vira um nome utilizavel em vez de chave invalida")
    void emptyNameGetsFallback() {
        assertThat(factory.normalizeFilename("")).isEqualTo("arquivo");
        assertThat(factory.normalizeFilename("   ")).isEqualTo("arquivo");
        assertThat(factory.normalizeFilename("...")).isEqualTo("arquivo");
    }

    @Test
    @DisplayName("nome muito longo e truncado")
    void longNameIsTruncated() {
        String name = "a".repeat(300) + ".pdf";
        String normalized = factory.normalizeFilename(name);

        assertThat(normalized).hasSizeLessThanOrEqualTo(124);
        assertThat(normalized).endsWith(".pdf");
    }

    @Test
    @DisplayName("caminho no nome do arquivo e descartado, nao vira travessia")
    void pathInFilenameIsStripped() {
        // Descartar tudo antes da ultima barra ja neutraliza a tentativa.
        assertThat(factory.normalizeFilename("../../etc/passwd")).isEqualTo("passwd");
    }

    @Test
    @DisplayName("travessia no subcaminho e recusada")
    void rejectsTraversalInRelativePath() {
        assertThatThrownBy(() -> factory.normalizeRelativePath("a/../../b"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid_relative_path");
    }

    @Test
    @DisplayName("nome com pontos seguidos e legitimo e nao e recusado")
    void allowsDoubleDotsInsideName() {
        // "relatorio..final" contem "..", mas nao sobe nivel nenhum.
        assertThat(factory.normalizeFilename("relatorio..final.pdf")).endsWith(".pdf");
    }

    @Test
    @DisplayName("descarta o caminho quando o navegador manda o nome completo")
    void stripsDirectoryFromFilename() {
        assertThat(factory.normalizeFilename("C:\\Users\\x\\planta.pdf")).isEqualTo("planta.pdf");
        assertThat(factory.normalizeFilename("pasta/planta.pdf")).isEqualTo("planta.pdf");
    }

    @Test
    @DisplayName("a chave gerada contem produto, colecao e um id unico")
    void keyLayout() {
        var key = factory.create("casa-confort-80m2", "ARCH", null, "Planta.pdf");

        assertThat(key.key())
                .startsWith("products/casa-confort-80m2/ARCH/")
                .endsWith("/planta.pdf")
                .contains(key.keySegment().toString());

        // Versionar e assunto do banco. O "/v1/" das chaves antigas era justamente o
        // residuo de quando o caminho era reconstruido por convencao.
        assertThat(key.key()).doesNotContain("/v1/");
    }

    @Test
    @DisplayName("dois uploads do mesmo nome nunca colidem")
    void keysNeverCollide() {
        var first = factory.create("p", "ARCH", null, "Planta.pdf");
        var second = factory.create("p", "ARCH", null, "Planta.pdf");

        assertThat(first.key()).isNotEqualTo(second.key());
    }

    @Test
    @DisplayName("subpastas do upload de pasta sao preservadas e normalizadas")
    void keepsRelativePath() {
        var key = factory.create("p", "APOIO", "Memorial Técnico/Anexos", "Descritivo.pdf");

        assertThat(key.key()).contains("/memorial-tecnico/anexos/");
        assertThat(key.key()).endsWith("/descritivo.pdf");
    }

    @Test
    @DisplayName("chave acima do limite da coluna e recusada")
    void rejectsOverlongKey() {
        String deepPath = String.join("/", java.util.Collections.nCopies(30, "subpasta-bem-longa"));

        assertThatThrownBy(() -> factory.create("produto", "COLECAO", deepPath, "arquivo.pdf"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("storage_key_too_long");
    }

    @Test
    @DisplayName("arquivo sem extensao nao quebra")
    void handlesMissingExtension() {
        assertThat(factory.normalizeFilename("LEIAME")).isEqualTo("leiame");
    }
}
