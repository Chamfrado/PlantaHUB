package com.plantahub.api.catalog;

import com.plantahub.api.domain.catalog.enums.ProductStatus;
import com.plantahub.api.service.ProductCatalogService;
import com.plantahub.api.support.AbstractApiTest;
import com.plantahub.api.support.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Uma colecao marcada como "acompanha toda oferta" existe, tem arquivos e precisa de
 * linha em product_plan_type para ancora-los — mas nunca pode virar uma opcao de compra.
 * Antes desta fase isso era garantido por um literal {@code "APOIO"} no codigo.
 */
@Transactional
class PurchasableCollectionTest extends AbstractApiTest {

    @Autowired private ProductCatalogService catalogService;
    @Autowired private TestDataFactory fixtures;

    @Test
    @DisplayName("colecao nao-compravel fica fora do seletor publico")
    void bundledCollectionIsHiddenFromSelector() {
        var product = fixtures.product(TestDataFactory.unique("p"), "casas", TestDataFactory.unique("s"), ProductStatus.PUBLISHED);

        var sellable = fixtures.collection(TestDataFactory.uniqueCode("SELL"));
        var attachment = fixtures.collection(
                TestDataFactory.uniqueCode("ATT"), false, true);

        fixtures.offer(product, sellable, 150000);
        fixtures.offer(product, attachment, 0);

        var codes = catalogService.getPlanTypes(product.getCategory(), product.getSlug()).stream()
                .map(o -> o.code())
                .toList();

        assertThat(codes).contains(sellable.getCode());
        assertThat(codes).doesNotContain(attachment.getCode());
    }

    @Test
    @DisplayName("colecao desativada some do seletor")
    void inactiveCollectionIsHidden() {
        var product = fixtures.product(TestDataFactory.unique("p"), "casas", TestDataFactory.unique("s"), ProductStatus.PUBLISHED);
        var collection = fixtures.collection(TestDataFactory.uniqueCode("OFF"));
        fixtures.offer(product, collection, 100000);

        collection.setActive(false);

        var codes = catalogService.getPlanTypes(product.getCategory(), product.getSlug()).stream()
                .map(o -> o.code())
                .toList();

        assertThat(codes).doesNotContain(collection.getCode());
    }
}
