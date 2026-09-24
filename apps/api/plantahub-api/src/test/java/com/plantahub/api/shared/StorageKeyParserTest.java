package com.plantahub.api.shared;

import com.plantahub.api.shared.storage.StorageKeyParser;
import com.plantahub.api.shared.storage.StorageKeyParser.Shape;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A mesma regra decide a colecao de um arquivo na reconciliacao do bucket legado e no
 * upload de uma pasta pelo painel. Se ela divergir entre os dois, "subir de novo a pasta
 * ARCH" produziria um resultado diferente de "reconciliar a pasta ARCH".
 */
class StorageKeyParserTest {

    @Test
    @DisplayName("chave simples de colecao")
    void parsesCollectionFile() {
        var parsed = StorageKeyParser.parse("products/casa-confort-80m2/ARCH/planta.pdf");

        assertThat(parsed.shape()).isEqualTo(Shape.COLLECTION_FILE);
        assertThat(parsed.productId()).isEqualTo("casa-confort-80m2");
        assertThat(parsed.folder()).isEqualTo("ARCH");
        assertThat(parsed.relativePath()).isNull();
        assertThat(parsed.filename()).isEqualTo("planta.pdf");
        assertThat(parsed.extension()).isEqualTo("pdf");
    }

    @Test
    @DisplayName("chave legada com segmento de versao vira subcaminho, nao se perde")
    void keepsLegacyVersionSegmentAsRelativePath() {
        var parsed = StorageKeyParser.parse(
                "products/casa-confort-80m2/ARCH/v1/planta-arquitetonica.pdf");

        assertThat(parsed.shape()).isEqualTo(Shape.COLLECTION_FILE);
        assertThat(parsed.folder()).isEqualTo("ARCH");
        assertThat(parsed.relativePath()).isEqualTo("v1");
        assertThat(parsed.filename()).isEqualTo("planta-arquitetonica.pdf");
    }

    @Test
    @DisplayName("subpastas profundas viram um subcaminho completo")
    void keepsDeepSubfolders() {
        var parsed = StorageKeyParser.parse(
                "products/chale-prime-85m2/APOIO/memorial/tecnico/descritivo.pdf");

        assertThat(parsed.folder()).isEqualTo("APOIO");
        assertThat(parsed.relativePath()).isEqualTo("memorial/tecnico");
        assertThat(parsed.filename()).isEqualTo("descritivo.pdf");
    }

    @Test
    @DisplayName("arquivo solto sob o produto nao tem colecao")
    void detectsFileDirectlyUnderProduct() {
        var parsed = StorageKeyParser.parse("products/casa-confort-80m2/cover.webp");

        assertThat(parsed.shape()).isEqualTo(Shape.DIRECTLY_UNDER_PRODUCT);
        assertThat(parsed.productId()).isEqualTo("casa-confort-80m2");
        assertThat(parsed.folder()).isNull();
        assertThat(parsed.filename()).isEqualTo("cover.webp");
        assertThat(parsed.isImage())
                .as("sem reconhecer imagem, a capa do produto viraria arquivo comprável")
                .isTrue();
    }

    @Test
    @DisplayName("extensoes de imagem sao reconhecidas independente de caixa")
    void recognizesImageExtensions() {
        assertThat(StorageKeyParser.parse("products/p/COVER.JPG").isImage()).isTrue();
        assertThat(StorageKeyParser.parse("products/p/foto.png").isImage()).isTrue();
        assertThat(StorageKeyParser.parse("products/p/planta.pdf").isImage()).isFalse();
        assertThat(StorageKeyParser.parse("products/p/desenho.dwg").isImage()).isFalse();
    }

    @Test
    @DisplayName("chaves fora do formato sao rejeitadas em vez de adivinhadas")
    void rejectsUnparseableKeys() {
        assertThat(StorageKeyParser.parse("bundles/abc/x.zip").shape()).isEqualTo(Shape.UNPARSEABLE);
        assertThat(StorageKeyParser.parse("products/").shape()).isEqualTo(Shape.UNPARSEABLE);
        assertThat(StorageKeyParser.parse("products/casa/").shape()).isEqualTo(Shape.UNPARSEABLE);
        assertThat(StorageKeyParser.parse("qualquer-coisa.pdf").shape()).isEqualTo(Shape.UNPARSEABLE);
        assertThat(StorageKeyParser.parse("").shape()).isEqualTo(Shape.UNPARSEABLE);
        assertThat(StorageKeyParser.parse(null).shape()).isEqualTo(Shape.UNPARSEABLE);
    }

    @Test
    @DisplayName("o nome da pasta e normalizado para comparar com o codigo da colecao")
    void normalizesFolderForComparison() {
        assertThat(StorageKeyParser.normalizeFolder("arch")).isEqualTo("ARCH");
        assertThat(StorageKeyParser.normalizeFolder(" Apoio ")).isEqualTo("APOIO");
        assertThat(StorageKeyParser.normalizeFolder(null)).isNull();
    }

    @Test
    @DisplayName("arquivo sem extensao nao quebra")
    void handlesFilesWithoutExtension() {
        var parsed = StorageKeyParser.parse("products/casa/ARCH/LEIAME");

        assertThat(parsed.filename()).isEqualTo("LEIAME");
        assertThat(parsed.extension()).isEmpty();
        assertThat(parsed.isImage()).isFalse();
    }
}
