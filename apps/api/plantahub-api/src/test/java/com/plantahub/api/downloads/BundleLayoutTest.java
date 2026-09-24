package com.plantahub.api.downloads;

import com.plantahub.api.domain.catalog.enums.ProductStatus;
import com.plantahub.api.domain.orders.enums.OrderStatus;
import com.plantahub.api.service.DownloadBundleService;
import com.plantahub.api.service.EntitlementPinningService;
import com.plantahub.api.support.AbstractApiTest;
import com.plantahub.api.support.InMemoryObjectStorage;
import com.plantahub.api.support.TestDataFactory;
import com.plantahub.api.web.dto.downloads.CreateDownloadBundleRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A arvore de pastas do ZIP reproduz o layout de antes — mas agora sai dos dados, e nao de
 * um caso especial escrito para uma colecao de nome fixo.
 */
@Transactional
class BundleLayoutTest extends AbstractApiTest {

    @Autowired private DownloadBundleService bundleService;
    @Autowired private EntitlementPinningService pinningService;
    @Autowired private InMemoryObjectStorage storage;
    @Autowired private TestDataFactory fixtures;

    @BeforeEach
    void setUp() {
        storage.clear();
    }

    private List<String> zipEntries(String storageKey) throws Exception {
        List<String> entries = new ArrayList<>();

        byte[] bytes;
        try (var in = storage.open(storageKey)) {
            bytes = in.readAllBytes();
        }

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entries.add(entry.getName());
                zis.closeEntry();
            }
        }

        return entries;
    }

    @Test
    @DisplayName("cada arquivo cai na pasta da sua propria colecao")
    void filesLandInTheirOwnCollectionFolder() throws Exception {
        var user = fixtures.user();
        var product = fixtures.product(TestDataFactory.unique("p"), "casas",
                TestDataFactory.unique("s"), ProductStatus.PUBLISHED);
        product.setName("Casa Confort");

        var sold = fixtures.collection(TestDataFactory.uniqueCode("ARCH"));
        var attachment = fixtures.collection(TestDataFactory.uniqueCode("APOIO"), false, true);

        var soldOffer = fixtures.offer(product, sold, 150000);
        var attachmentOffer = fixtures.offer(product, attachment, 0, false);

        var planta = fixtures.asset(soldOffer, "planta.pdf");
        var memorial = fixtures.asset(attachmentOffer, "memorial.pdf");

        storage.seed(planta.getStorageKey(), "conteudo-planta");
        storage.seed(memorial.getStorageKey(), "conteudo-memorial");

        var order = fixtures.order(user, OrderStatus.PAID, product, sold, 150000);
        pinningService.pin(fixtures.entitlement(user, order, product, sold));

        var response = bundleService.createBundle(user.getEmail(), new CreateDownloadBundleRequest(
                List.of(new CreateDownloadBundleRequest.Item(product.getId(), List.of(sold.getCode())))));

        assertThat(zipEntries(response.storageKey()))
                .as("o arquivo que acompanha a oferta vai para a pasta DELE, "
                        + "sem nenhum caso especial no codigo")
                .containsExactlyInAnyOrder(
                        "Casa Confort/" + sold.getCode() + "/planta.pdf",
                        "Casa Confort/" + attachment.getCode() + "/memorial.pdf");
    }

    @Test
    @DisplayName("subpastas do arquivo sao preservadas na arvore do ZIP")
    void preservesRelativePath() throws Exception {
        var user = fixtures.user();
        var product = fixtures.product(TestDataFactory.unique("p"), "casas",
                TestDataFactory.unique("s"), ProductStatus.PUBLISHED);
        product.setName("Chale Prime");

        var collection = fixtures.collection(TestDataFactory.uniqueCode("ARCH"));
        var offer = fixtures.offer(product, collection, 150000);

        var asset = fixtures.asset(offer, "planta.pdf");
        asset.setRelativePath("pranchas/nivel-1");

        storage.seed(asset.getStorageKey(), "conteudo");

        var order = fixtures.order(user, OrderStatus.PAID, product, collection, 150000);
        pinningService.pin(fixtures.entitlement(user, order, product, collection));

        var response = bundleService.createBundle(user.getEmail(), new CreateDownloadBundleRequest(
                List.of(new CreateDownloadBundleRequest.Item(product.getId(), List.of(collection.getCode())))));

        assertThat(zipEntries(response.storageKey()))
                .containsExactly("Chale Prime/" + collection.getCode() + "/pranchas/nivel-1/planta.pdf");
    }

    @Test
    @DisplayName("o ZIP fica sob private/, fora do alcance de qualquer politica publica")
    void bundlesArePrivate() {
        var user = fixtures.user();
        var product = fixtures.product(TestDataFactory.unique("p"), "casas",
                TestDataFactory.unique("s"), ProductStatus.PUBLISHED);
        var collection = fixtures.collection(TestDataFactory.uniqueCode("COL"));
        var offer = fixtures.offer(product, collection, 150000);

        var asset = fixtures.asset(offer, "planta.pdf");
        storage.seed(asset.getStorageKey(), "conteudo");

        var order = fixtures.order(user, OrderStatus.PAID, product, collection, 150000);
        pinningService.pin(fixtures.entitlement(user, order, product, collection));

        var response = bundleService.createBundle(user.getEmail(), new CreateDownloadBundleRequest(
                List.of(new CreateDownloadBundleRequest.Item(product.getId(), List.of(collection.getCode())))));

        assertThat(response.storageKey()).startsWith("private/bundles/");
    }

    @Test
    @DisplayName("o mesmo pedido reaproveita o ZIP ja gerado")
    void reusesCachedBundle() {
        var user = fixtures.user();
        var product = fixtures.product(TestDataFactory.unique("p"), "casas",
                TestDataFactory.unique("s"), ProductStatus.PUBLISHED);
        var collection = fixtures.collection(TestDataFactory.uniqueCode("COL"));
        var offer = fixtures.offer(product, collection, 150000);

        var asset = fixtures.asset(offer, "planta.pdf");
        storage.seed(asset.getStorageKey(), "conteudo");

        var order = fixtures.order(user, OrderStatus.PAID, product, collection, 150000);
        pinningService.pin(fixtures.entitlement(user, order, product, collection));

        var request = new CreateDownloadBundleRequest(
                List.of(new CreateDownloadBundleRequest.Item(product.getId(), List.of(collection.getCode()))));

        var first = bundleService.createBundle(user.getEmail(), request);
        var second = bundleService.createBundle(user.getEmail(), request);

        assertThat(second.storageKey()).isEqualTo(first.storageKey());
    }
}
