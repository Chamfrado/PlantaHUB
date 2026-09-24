package com.plantahub.api.entitlements;

import com.plantahub.api.domain.catalog.enums.ProductStatus;
import com.plantahub.api.domain.orders.enums.OrderStatus;
import com.plantahub.api.service.EntitlementService;
import com.plantahub.api.service.LibraryService;
import com.plantahub.api.service.ProductCatalogService;
import com.plantahub.api.support.AbstractApiTest;
import com.plantahub.api.support.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Arquivar tira o produto de venda e nao pode quebrar nada de quem ja comprou.
 *
 * <p>E a diferenca central entre ARCHIVED e DRAFT, e a razao de o ciclo de vida ser um
 * enum e nao o boolean {@code active} que existia antes.
 */
@Transactional
class ArchivedProductResolutionTest extends AbstractApiTest {

    @Autowired private ProductCatalogService catalogService;
    @Autowired private EntitlementService entitlementService;
    @Autowired private LibraryService libraryService;
    @Autowired private TestDataFactory fixtures;

    @Test
    @DisplayName("arquivar remove do catalogo mas preserva biblioteca e download")
    void archivingDoesNotBreakPastPurchases() {
        var user = fixtures.user();
        var product = fixtures.product(TestDataFactory.unique("p"), "casas", TestDataFactory.unique("s"), ProductStatus.PUBLISHED);
        var collection = fixtures.collection(TestDataFactory.uniqueCode("COL"));
        fixtures.offer(product, collection, 150000);

        var order = fixtures.order(user, OrderStatus.PAID, product, collection, 150000);
        fixtures.entitlement(user, order, product, collection);

        product.applyStatus(ProductStatus.ARCHIVED);

        // Sai do catalogo publico...
        assertThatThrownBy(() -> catalogService.getProduct(product.getCategory(), product.getSlug()))
                .hasMessageContaining("product_not_found");

        // ...mas o comprador continua com o direito...
        assertThat(entitlementService.validateEntitlement(
                user.getEmail(), product.getId(), collection.getCode())).isNotNull();

        // ...e o produto continua aparecendo na biblioteca dele.
        var library = libraryService.myLibrary(user.getEmail());
        assertThat(library)
                .extracting(p -> p.productId())
                .contains(product.getId());
    }
}
