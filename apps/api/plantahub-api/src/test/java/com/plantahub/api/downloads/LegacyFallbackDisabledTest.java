package com.plantahub.api.downloads;

import com.plantahub.api.domain.catalog.enums.ProductStatus;
import com.plantahub.api.domain.orders.enums.OrderStatus;
import com.plantahub.api.service.DownloadService;
import com.plantahub.api.service.EntitlementPinningService;
import com.plantahub.api.support.AbstractApiTest;
import com.plantahub.api.support.InMemoryObjectStorage;
import com.plantahub.api.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Estado final, depois do cutover: o banco e a unica fonte da verdade.
 *
 * <p>Com a flag desligada, um arquivo que existe no bucket mas nao esta pinado nao e
 * entregue. E por isso que desligar a flag antes de o backfill reportar
 * {@code entitlementsWithNoAssets} vazio tiraria o download de compradores reais.
 */
@Transactional
@TestPropertySource(properties = "app.downloads.legacy-fallback=false")
class LegacyFallbackDisabledTest extends AbstractApiTest {

    @Autowired private DownloadService downloadService;
    @Autowired private EntitlementPinningService pinningService;
    @Autowired private InMemoryObjectStorage storage;
    @Autowired private TestDataFactory fixtures;

    @BeforeEach
    void setUp() {
        storage.clear();
    }

    @Test
    @DisplayName("sem pins, nada e entregue mesmo com o arquivo no bucket")
    void unpinnedPurchaseGetsNothing() {
        var user = fixtures.user();
        var product = fixtures.product(TestDataFactory.unique("p"), "casas",
                TestDataFactory.unique("s"), ProductStatus.PUBLISHED);
        var collection = fixtures.collection("ARCHOFF");
        fixtures.offer(product, collection, 150000);

        var order = fixtures.order(user, OrderStatus.PAID, product, collection, 150000);
        fixtures.entitlement(user, order, product, collection);

        storage.seed("products/" + product.getId() + "/ARCHOFF/planta.pdf", "conteudo");

        var response = downloadService.downloadAll(user.getEmail(), product.getId(), collection.getCode());

        assertThat(response.files())
                .as("depois do cutover, o que nao esta no banco nao existe")
                .isEmpty();
    }

    @Test
    @DisplayName("com pins, a entrega funciona normalmente")
    void pinnedPurchaseWorks() {
        var user = fixtures.user();
        var product = fixtures.product(TestDataFactory.unique("p"), "casas",
                TestDataFactory.unique("s"), ProductStatus.PUBLISHED);
        var collection = fixtures.collection(TestDataFactory.uniqueCode("COL"));
        var offer = fixtures.offer(product, collection, 150000);

        fixtures.asset(offer, "planta.pdf");

        var order = fixtures.order(user, OrderStatus.PAID, product, collection, 150000);
        pinningService.pin(fixtures.entitlement(user, order, product, collection));

        var response = downloadService.downloadAll(user.getEmail(), product.getId(), collection.getCode());

        assertThat(response.files())
                .extracting(f -> f.filename())
                .containsExactly("planta.pdf");
    }
}
