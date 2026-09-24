package com.plantahub.api.domain.catalog;

import com.plantahub.api.domain.catalog.enums.ProductStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Invariantes de {@code Product.applyStatus}.
 *
 * <p>Enquanto a coluna {@code active} existir, ela e {@code status} descrevem a mesma
 * coisa em dois lugares — e duas fontes da verdade sempre divergem. Estes testes travam
 * a sincronia no unico metodo que pode mudar o status.
 */
class ProductStatusTest {

    private Product newProduct() {
        Instant now = Instant.now();
        Product p = new Product();
        p.setId("casa-teste-80m2");
        p.setSlug("teste");
        p.setCategory("casas");
        p.setName("Teste");
        p.setShortDesc("Produto de teste");
        p.setAreaM2(80);
        p.setBasePriceCents(0);
        p.setCustomizable(true);
        p.setActive(false);
        p.setStatus(ProductStatus.DRAFT);
        p.setCreatedAt(now);
        p.setUpdatedAt(now);
        return p;
    }

    @Test
    @DisplayName("publicar liga active e carimba publishedAt")
    void publishSyncsActiveAndStamps() {
        Product p = newProduct();

        p.applyStatus(ProductStatus.PUBLISHED);

        assertThat(p.getStatus()).isEqualTo(ProductStatus.PUBLISHED);
        assertThat(p.getActive()).isTrue();
        assertThat(p.isPublished()).isTrue();
        assertThat(p.getPublishedAt()).isNotNull();
        assertThat(p.getArchivedAt()).isNull();
    }

    @Test
    @DisplayName("publishedAt guarda a primeira publicacao, nao a ultima")
    void publishedAtIsNotOverwritten() {
        Product p = newProduct();

        p.applyStatus(ProductStatus.PUBLISHED);
        Instant first = p.getPublishedAt();

        p.applyStatus(ProductStatus.DRAFT);
        p.applyStatus(ProductStatus.PUBLISHED);

        assertThat(p.getPublishedAt()).isEqualTo(first);
    }

    @Test
    @DisplayName("despublicar desliga active")
    void unpublishSyncsActive() {
        Product p = newProduct();
        p.applyStatus(ProductStatus.PUBLISHED);

        p.applyStatus(ProductStatus.DRAFT);

        assertThat(p.getStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(p.getActive()).isFalse();
        assertThat(p.isPublished()).isFalse();
    }

    @Test
    @DisplayName("arquivar desliga active e carimba archivedAt sem apagar publishedAt")
    void archiveKeepsHistory() {
        Product p = newProduct();
        p.applyStatus(ProductStatus.PUBLISHED);
        Instant publishedAt = p.getPublishedAt();

        p.applyStatus(ProductStatus.ARCHIVED);

        assertThat(p.getStatus()).isEqualTo(ProductStatus.ARCHIVED);
        assertThat(p.getActive()).isFalse();
        assertThat(p.getArchivedAt()).isNotNull();
        // O historico de quando o produto esteve a venda nao pode ser perdido:
        // pedidos antigos continuam apontando para ele.
        assertThat(p.getPublishedAt()).isEqualTo(publishedAt);
    }

    @Test
    @DisplayName("active nunca fica true fora de PUBLISHED")
    void activeOnlyForPublished() {
        Product p = newProduct();

        for (ProductStatus status : ProductStatus.values()) {
            p.applyStatus(status);
            assertThat(p.getActive())
                    .as("active para status %s", status)
                    .isEqualTo(status == ProductStatus.PUBLISHED);
        }
    }

    @Test
    @DisplayName("qualquer mudanca de status atualiza updatedAt")
    void statusChangeTouchesUpdatedAt() {
        Product p = newProduct();
        p.setUpdatedAt(Instant.parse("2020-01-01T00:00:00Z"));

        p.applyStatus(ProductStatus.PUBLISHED);

        assertThat(p.getUpdatedAt()).isAfter(Instant.parse("2020-01-01T00:00:00Z"));
    }
}
