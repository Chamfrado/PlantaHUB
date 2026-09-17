package com.plantahub.api.catalog;

import com.plantahub.api.domain.catalog.enums.ProductStatus;
import com.plantahub.api.repository.ProductRepository;
import com.plantahub.api.service.ProductCatalogService;
import com.plantahub.api.support.AbstractApiTest;
import com.plantahub.api.support.TestDataFactory;
import com.plantahub.api.web.dto.catalog.ProductSummaryDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O preço que a vitrine anuncia.
 *
 * <p>Ele deriva das ofertas, e não da coluna {@code product.base_price_cents}. A coluna é
 * anterior ao catálogo administrável: desde que o preço passou a ser por coleção, nada mais
 * a escreve — e ela ficou em zero em todos os produtos, fazendo a home anunciar R$ 0,00
 * para produtos com cinco ofertas pagas.
 *
 * <p>Derivar não é só corrigir o número: é tirar do caminho uma classe inteira de bug, a de
 * dois lugares discordando sobre o mesmo preço.
 */
@Transactional
class CatalogPriceTest extends AbstractApiTest {

    @Autowired private ProductCatalogService catalog;
    @Autowired private ProductRepository productRepo;
    @Autowired private TestDataFactory fixtures;

    private ProductSummaryDTO summaryOf(String productId) {
        return catalog.listProducts(null, null).stream()
                .filter(p -> p.id().equals(productId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("produto ausente da vitrine: " + productId));
    }

    @Test
    @DisplayName("anuncia a oferta mais barata, ignorando a coluna antiga")
    void announcesTheCheapestOffer() {
        var product = fixtures.product(TestDataFactory.unique("p"), "casas",
                TestDataFactory.unique("s"), ProductStatus.PUBLISHED);

        fixtures.offer(product, fixtures.collection(TestDataFactory.uniqueCode("ARCH")), 150000);
        fixtures.offer(product, fixtures.collection(TestDataFactory.uniqueCode("LAND")), 25000);
        fixtures.offer(product, fixtures.collection(TestDataFactory.uniqueCode("STR")), 200000);

        // A coluna continua em zero, como esta em producao nos seis produtos semeados.
        assertThat(product.getBasePriceCents()).isZero();

        assertThat(summaryOf(product.getId()).basePriceCents())
                .as("ler a coluna daria R$ 0,00 num produto com tres ofertas pagas")
                .isEqualTo(25000);
    }

    @Test
    @DisplayName("uma coluna desatualizada nao contamina a vitrine")
    void staleColumnIsIgnored() {
        var product = fixtures.product(TestDataFactory.unique("p"), "casas",
                TestDataFactory.unique("s"), ProductStatus.PUBLISHED);

        fixtures.offer(product, fixtures.collection(TestDataFactory.uniqueCode("ARCH")), 30000);

        product.setBasePriceCents(999999);
        productRepo.save(product);

        assertThat(summaryOf(product.getId()).basePriceCents())
                .as("a fonte da verdade sobre preco e a oferta, e so ela")
                .isEqualTo(30000);
    }

    @Test
    @DisplayName("oferta fora de venda nao vira o preco anunciado")
    void unavailableOfferIsNotAdvertised() {
        var product = fixtures.product(TestDataFactory.unique("p"), "casas",
                TestDataFactory.unique("s"), ProductStatus.PUBLISHED);

        fixtures.offer(product, fixtures.collection(TestDataFactory.uniqueCode("ARCH")), 80000);
        fixtures.offer(product, fixtures.collection(TestDataFactory.uniqueCode("PROMO")), 1000, false);

        assertThat(summaryOf(product.getId()).basePriceCents())
                .as("anunciar um preco que o cliente nao consegue comprar e propaganda enganosa")
                .isEqualTo(80000);
    }

    @Test
    @DisplayName("colecao acompanhante nao puxa o preco para zero")
    void bundledCollectionDoesNotDragThePriceDown() {
        var product = fixtures.product(TestDataFactory.unique("p"), "casas",
                TestDataFactory.unique("s"), ProductStatus.PUBLISHED);

        fixtures.offer(product, fixtures.collection(TestDataFactory.uniqueCode("ARCH")), 120000);

        // APOIO acompanha toda compra e custa zero. Sem excluir o que nao e comprável, o
        // minimo seria sempre zero e todo produto anunciaria "de graca".
        fixtures.offer(product, fixtures.collection(TestDataFactory.uniqueCode("APOIO"), false, true), 0);

        assertThat(summaryOf(product.getId()).basePriceCents()).isEqualTo(120000);
    }

    @Test
    @DisplayName("produto sem oferta a venda fica sem preco, e nao com preco zero")
    void productWithoutOffersHasNoPrice() {
        var product = fixtures.product(TestDataFactory.unique("p"), "casas",
                TestDataFactory.unique("s"), ProductStatus.PUBLISHED);

        assertThat(summaryOf(product.getId()).basePriceCents())
                .as("nulo e 'sem preco', que a vitrine mostra como 'sob consulta'; "
                        + "zero seria 'de graca'")
                .isNull();
    }

    @Test
    @DisplayName("a pagina de detalhe usa o mesmo preco da vitrine")
    void detailMatchesTheShowcase() {
        var product = fixtures.product(TestDataFactory.unique("p"), "casas",
                TestDataFactory.unique("s"), ProductStatus.PUBLISHED);

        fixtures.offer(product, fixtures.collection(TestDataFactory.uniqueCode("ARCH")), 45000);
        fixtures.offer(product, fixtures.collection(TestDataFactory.uniqueCode("LAND")), 15000);

        var detail = catalog.getProduct(product.getCategory(), product.getSlug());

        assertThat(detail.basePriceCents())
                .as("card e pagina discordarem sobre o preco e pior do que os dois errados")
                .isEqualTo(summaryOf(product.getId()).basePriceCents())
                .isEqualTo(15000);
    }
}
