package com.plantahub.api.downloads;

import com.plantahub.api.domain.catalog.enums.ProductStatus;
import com.plantahub.api.domain.orders.enums.OrderStatus;
import com.plantahub.api.service.EntitlementPinningService;
import com.plantahub.api.service.LibraryService;
import com.plantahub.api.support.AbstractApiTest;
import com.plantahub.api.support.InMemoryObjectStorage;
import com.plantahub.api.support.TestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A biblioteca fazia uma listagem completa no S3 <b>por produto, a cada requisicao</b>.
 * Agora sai de duas consultas.
 */
@Transactional
class LibraryReadsFromDatabaseTest extends AbstractApiTest {

    @Autowired private LibraryService libraryService;
    @Autowired private EntitlementPinningService pinningService;
    @Autowired private InMemoryObjectStorage storage;
    @Autowired private TestDataFactory fixtures;

    @BeforeEach
    void setUp() {
        storage.clear();
    }

    @AfterEach
    void tearDown() {
        storage.failOnList(false);
    }

    @Test
    @DisplayName("monta a biblioteca sem tocar no bucket")
    void doesNotTouchStorage() {
        var user = fixtures.user();
        var product = fixtures.product(TestDataFactory.unique("p"), "casas",
                TestDataFactory.unique("s"), ProductStatus.PUBLISHED);
        var collection = fixtures.collection(TestDataFactory.uniqueCode("COL"));
        var offer = fixtures.offer(product, collection, 150000);

        var asset = fixtures.asset(offer, "planta.pdf");

        var order = fixtures.order(user, OrderStatus.PAID, product, collection, 150000);
        pinningService.pin(fixtures.entitlement(user, order, product, collection));

        // Qualquer listagem no S3 a partir daqui estoura o teste.
        storage.failOnList(true);

        var library = libraryService.myLibrary(user.getEmail());

        assertThat(library).hasSize(1);
        assertThat(library.get(0).planTypes()).hasSize(1);
        assertThat(library.get(0).planTypes().get(0).assets())
                .extracting(a -> a.filename())
                .containsExactly("planta.pdf");

        // Id real da linha, e nao mais um hash do caminho do arquivo.
        assertThat(library.get(0).planTypes().get(0).assets().get(0).id())
                .isEqualTo(asset.getId().toString());
    }

    @Test
    @DisplayName("usa os metadados reais do arquivo, nao valores inventados")
    void usesRealAssetMetadata() {
        var user = fixtures.user();
        var product = fixtures.product(TestDataFactory.unique("p"), "casas",
                TestDataFactory.unique("s"), ProductStatus.PUBLISHED);
        var collection = fixtures.collection(TestDataFactory.uniqueCode("COL"));
        var offer = fixtures.offer(product, collection, 150000);

        var asset = fixtures.asset(offer, "planta.pdf");
        asset.setVersion(3);

        var order = fixtures.order(user, OrderStatus.PAID, product, collection, 150000);
        pinningService.pin(fixtures.entitlement(user, order, product, collection));

        storage.failOnList(true);

        var libraryAsset = libraryService.myLibrary(user.getEmail())
                .get(0).planTypes().get(0).assets().get(0);

        assertThat(libraryAsset.version())
                .as("a versao vinha fixa em 1 porque nao havia linha no banco para consultar")
                .isEqualTo(3);
        assertThat(libraryAsset.sizeBytes()).isEqualTo(asset.getSizeBytes());
        assertThat(libraryAsset.storageKey()).isEqualTo(asset.getStorageKey());
    }
}
