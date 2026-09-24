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
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O download passa a ler de {@code entitlement_asset}. Antes ele listava prefixos no S3 e
 * montava chaves por convencao — a origem dos {@code NoSuchKey}.
 */
@Transactional
class DownloadFromPinsTest extends AbstractApiTest {

    @Autowired private DownloadService downloadService;
    @Autowired private EntitlementPinningService pinningService;
    @Autowired private InMemoryObjectStorage storage;
    @Autowired private TestDataFactory fixtures;

    @BeforeEach
    void setUp() {
        storage.clear();
    }

    @Test
    @DisplayName("entrega os arquivos pinados, incluindo os que acompanham toda oferta")
    void servesPinnedAssets() {
        var user = fixtures.user();
        var product = fixtures.product(TestDataFactory.unique("p"), "casas",
                TestDataFactory.unique("s"), ProductStatus.PUBLISHED);

        var sold = fixtures.collection(TestDataFactory.uniqueCode("SOLD"));
        var attachment = fixtures.collection(TestDataFactory.uniqueCode("ATT"), false, true);

        var soldOffer = fixtures.offer(product, sold, 150000);
        var attachmentOffer = fixtures.offer(product, attachment, 0, false);

        fixtures.asset(soldOffer, "planta.pdf");
        fixtures.asset(attachmentOffer, "memorial.pdf");

        var order = fixtures.order(user, OrderStatus.PAID, product, sold, 150000);
        pinningService.pin(fixtures.entitlement(user, order, product, sold));

        var response = downloadService.downloadAll(user.getEmail(), product.getId(), sold.getCode());

        assertThat(response.files())
                .extracting(f -> f.filename())
                .containsExactlyInAnyOrder("planta.pdf", "memorial.pdf");

        assertThat(response.files())
                .allSatisfy(f -> assertThat(f.url()).isNotBlank());
    }

    @Test
    @DisplayName("arquivo removido do catalogo continua baixando para quem ja pagou")
    void softDeletedAssetStillDownloads() {
        var user = fixtures.user();
        var product = fixtures.product(TestDataFactory.unique("p"), "casas",
                TestDataFactory.unique("s"), ProductStatus.PUBLISHED);
        var collection = fixtures.collection(TestDataFactory.uniqueCode("COL"));
        var offer = fixtures.offer(product, collection, 150000);

        var asset = fixtures.asset(offer, "planta.pdf");

        var order = fixtures.order(user, OrderStatus.PAID, product, collection, 150000);
        pinningService.pin(fixtures.entitlement(user, order, product, collection));

        // O admin remove o arquivo do catalogo depois da venda.
        asset.setDeletedAt(Instant.now());

        var response = downloadService.downloadAll(user.getEmail(), product.getId(), collection.getCode());

        assertThat(response.files())
                .as("filtrar deletedAt aqui quebraria, em silencio, o download de quem ja comprou")
                .extracting(f -> f.filename())
                .containsExactly("planta.pdf");
    }

    @Test
    @DisplayName("a URL assinada aponta para a chave literal gravada, nao para uma montada")
    void presignsTheStoredKey() {
        var user = fixtures.user();
        var product = fixtures.product(TestDataFactory.unique("p"), "casas",
                TestDataFactory.unique("s"), ProductStatus.PUBLISHED);
        var collection = fixtures.collection(TestDataFactory.uniqueCode("COL"));
        var offer = fixtures.offer(product, collection, 150000);

        var asset = fixtures.asset(offer, "planta.pdf");

        var order = fixtures.order(user, OrderStatus.PAID, product, collection, 150000);
        pinningService.pin(fixtures.entitlement(user, order, product, collection));

        var response = downloadService.downloadAll(user.getEmail(), product.getId(), collection.getCode());

        assertThat(response.files()).hasSize(1);
        assertThat(response.files().get(0).storageKey()).isEqualTo(asset.getStorageKey());
        assertThat(response.files().get(0).url()).contains(asset.getStorageKey());
    }

    @Test
    @DisplayName("nao entrega arquivos de outra colecao do mesmo produto")
    void doesNotLeakOtherCollections() {
        var user = fixtures.user();
        var product = fixtures.product(TestDataFactory.unique("p"), "casas",
                TestDataFactory.unique("s"), ProductStatus.PUBLISHED);

        var bought = fixtures.collection(TestDataFactory.uniqueCode("BUY"));
        var other = fixtures.collection(TestDataFactory.uniqueCode("OTHER"));

        var boughtOffer = fixtures.offer(product, bought, 150000);
        var otherOffer = fixtures.offer(product, other, 90000);

        fixtures.asset(boughtOffer, "comprado.pdf");
        fixtures.asset(otherOffer, "nao-comprado.pdf");

        var order = fixtures.order(user, OrderStatus.PAID, product, bought, 150000);
        pinningService.pin(fixtures.entitlement(user, order, product, bought));

        var response = downloadService.downloadAll(user.getEmail(), product.getId(), bought.getCode());

        assertThat(response.files())
                .extracting(f -> f.filename())
                .containsExactly("comprado.pdf");
    }
}
