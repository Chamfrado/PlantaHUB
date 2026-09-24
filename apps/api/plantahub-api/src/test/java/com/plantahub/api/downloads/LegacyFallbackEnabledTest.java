package com.plantahub.api.downloads;

import com.plantahub.api.domain.catalog.enums.ProductStatus;
import com.plantahub.api.domain.orders.enums.OrderStatus;
import com.plantahub.api.service.DownloadService;
import com.plantahub.api.service.LibraryService;
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
 * Com a flag ligada, uma compra anterior a pinagem continua baixando exatamente como
 * antes. E o que impede uma regressao silenciosa entre o deploy e o backfill.
 */
@Transactional
@TestPropertySource(properties = "app.downloads.legacy-fallback=true")
class LegacyFallbackEnabledTest extends AbstractApiTest {

    @Autowired private DownloadService downloadService;
    @Autowired private LibraryService libraryService;
    @Autowired private InMemoryObjectStorage storage;
    @Autowired private TestDataFactory fixtures;

    @BeforeEach
    void setUp() {
        storage.clear();
    }

    @Test
    @DisplayName("compra sem arquivos pinados ainda baixa, pelo caminho antigo")
    void unpinnedPurchaseStillDownloads() {
        var user = fixtures.user();
        var product = fixtures.product(TestDataFactory.unique("p"), "casas",
                TestDataFactory.unique("s"), ProductStatus.PUBLISHED);
        var collection = fixtures.collection("ARCHLEGACY");
        fixtures.offer(product, collection, 150000);

        var order = fixtures.order(user, OrderStatus.PAID, product, collection, 150000);
        // Direito concedido, mas sem nenhum arquivo pinado: exatamente o estado de uma
        // compra feita antes desta modernizacao.
        fixtures.entitlement(user, order, product, collection);

        storage.seed("products/" + product.getId() + "/ARCHLEGACY/planta.pdf", "conteudo");
        storage.seed("products/" + product.getId() + "/APOIO/memorial.pdf", "conteudo");

        var response = downloadService.downloadAll(user.getEmail(), product.getId(), collection.getCode());

        assertThat(response.files())
                .as("a regra antiga, inclusive a pasta de apoio, sobrevive dentro do fallback")
                .extracting(f -> f.filename())
                .containsExactlyInAnyOrder("planta.pdf", "memorial.pdf");
    }

    @Test
    @DisplayName("a biblioteca tambem mostra a compra nao pinada")
    void unpinnedPurchaseStillAppearsInLibrary() {
        var user = fixtures.user();
        var product = fixtures.product(TestDataFactory.unique("p"), "casas",
                TestDataFactory.unique("s"), ProductStatus.PUBLISHED);
        var collection = fixtures.collection("ARCHLEG2");
        fixtures.offer(product, collection, 150000);

        var order = fixtures.order(user, OrderStatus.PAID, product, collection, 150000);
        fixtures.entitlement(user, order, product, collection);

        storage.seed("products/" + product.getId() + "/ARCHLEG2/planta.pdf", "conteudo");

        var library = libraryService.myLibrary(user.getEmail());

        assertThat(library).hasSize(1);
        assertThat(library.get(0).planTypes().get(0).assets())
                .extracting(a -> a.filename())
                .containsExactly("planta.pdf");
    }
}
