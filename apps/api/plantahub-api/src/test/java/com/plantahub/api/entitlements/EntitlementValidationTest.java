package com.plantahub.api.entitlements;

import com.plantahub.api.domain.catalog.enums.ProductStatus;
import com.plantahub.api.domain.orders.enums.OrderStatus;
import com.plantahub.api.service.EntitlementService;
import com.plantahub.api.support.AbstractApiTest;
import com.plantahub.api.support.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O portao do download de um plano avulso.
 *
 * <p>A implementacao anterior carregava todos os entitlements do usuario e filtrava em
 * memoria <b>sem</b> olhar o status do pedido, entao um direito pendurado num pedido nao
 * pago rendia URL assinada. O bundle e a biblioteca ja checavam PAID; so este caminho nao.
 */
@Transactional
class EntitlementValidationTest extends AbstractApiTest {

    @Autowired private EntitlementService entitlementService;
    @Autowired private TestDataFactory fixtures;

    @Test
    @DisplayName("entitlement de pedido PENDENTE nao libera download")
    void pendingOrderDoesNotGrantDownload() {
        var user = fixtures.user();
        var product = fixtures.product(TestDataFactory.unique("p"), "casas", TestDataFactory.unique("s"), ProductStatus.PUBLISHED);
        var collection = fixtures.collection(TestDataFactory.uniqueCode("COL"));
        fixtures.offer(product, collection, 150000);

        var order = fixtures.order(user, OrderStatus.PENDING, product, collection, 150000);
        fixtures.entitlement(user, order, product, collection);

        assertThatThrownBy(() -> entitlementService.validateEntitlement(
                user.getEmail(), product.getId(), collection.getCode()))
                .hasMessageContaining("no_entitlement");
    }

    @Test
    @DisplayName("entitlement de pedido PAGO libera download")
    void paidOrderGrantsDownload() {
        var user = fixtures.user();
        var product = fixtures.product(TestDataFactory.unique("p"), "casas", TestDataFactory.unique("s"), ProductStatus.PUBLISHED);
        var collection = fixtures.collection(TestDataFactory.uniqueCode("COL"));
        fixtures.offer(product, collection, 150000);

        var order = fixtures.order(user, OrderStatus.PAID, product, collection, 150000);
        fixtures.entitlement(user, order, product, collection);

        var entitlement = entitlementService.validateEntitlement(
                user.getEmail(), product.getId(), collection.getCode());

        assertThat(entitlement.getProduct().getId()).isEqualTo(product.getId());
        assertThat(entitlement.getPlanType().getCode()).isEqualTo(collection.getCode());
    }

    @Test
    @DisplayName("o codigo da colecao e case-insensitive na validacao")
    void collectionCodeIsCaseInsensitive() {
        var user = fixtures.user();
        var product = fixtures.product(TestDataFactory.unique("p"), "casas", TestDataFactory.unique("s"), ProductStatus.PUBLISHED);
        var collection = fixtures.collection(TestDataFactory.uniqueCode("COL"));
        fixtures.offer(product, collection, 150000);

        var order = fixtures.order(user, OrderStatus.PAID, product, collection, 150000);
        fixtures.entitlement(user, order, product, collection);

        assertThat(entitlementService.validateEntitlement(
                user.getEmail(), product.getId(), collection.getCode().toLowerCase())).isNotNull();
    }

    @Test
    @DisplayName("entitlement revogado nao libera download")
    void revokedEntitlementDoesNotGrantDownload() {
        var user = fixtures.user();
        var product = fixtures.product(TestDataFactory.unique("p"), "casas", TestDataFactory.unique("s"), ProductStatus.PUBLISHED);
        var collection = fixtures.collection(TestDataFactory.uniqueCode("COL"));
        fixtures.offer(product, collection, 150000);

        var order = fixtures.order(user, OrderStatus.PAID, product, collection, 150000);
        var entitlement = fixtures.entitlement(user, order, product, collection);
        entitlement.setRevokedAt(java.time.Instant.now());

        assertThatThrownBy(() -> entitlementService.validateEntitlement(
                user.getEmail(), product.getId(), collection.getCode()))
                .hasMessageContaining("no_entitlement");
    }
}
