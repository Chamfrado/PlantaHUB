package com.plantahub.api.reconciliation;

import com.plantahub.api.domain.catalog.enums.ProductStatus;
import com.plantahub.api.domain.orders.enums.OrderStatus;
import com.plantahub.api.repository.EntitlementAssetRepository;
import com.plantahub.api.service.admin.EntitlementBackfillService;
import com.plantahub.api.support.AbstractApiTest;
import com.plantahub.api.support.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O backfill pina as compras feitas antes de a pinagem existir.
 *
 * <p>O numero que importa aqui e {@code entitlementsWithNoAssets}: enquanto ele nao for
 * zero, desligar a flag de compatibilidade deixaria compradores reais sem download.
 */
@Transactional
class EntitlementBackfillTest extends AbstractApiTest {

    @Autowired private EntitlementBackfillService backfillService;
    @Autowired private EntitlementAssetRepository entitlementAssetRepo;
    @Autowired private TestDataFactory fixtures;

    private record Purchase(String productId, java.util.UUID entitlementId) {}

    private Purchase legacyPurchaseWithFiles(int fileCount) {
        var user = fixtures.user();
        var product = fixtures.product(TestDataFactory.unique("p"), "casas",
                TestDataFactory.unique("s"), ProductStatus.PUBLISHED);
        var collection = fixtures.collection(TestDataFactory.uniqueCode("COL"));
        var offer = fixtures.offer(product, collection, 150000);

        for (int i = 0; i < fileCount; i++) {
            fixtures.asset(offer, "arquivo-" + i + ".pdf");
        }

        var order = fixtures.order(user, OrderStatus.PAID, product, collection, 150000);
        var entitlement = fixtures.entitlement(user, order, product, collection);

        return new Purchase(product.getId(), entitlement.getId());
    }

    @Test
    @DisplayName("o ensaio conta sem gravar nada")
    void dryRunDoesNotWrite() {
        var purchase = legacyPurchaseWithFiles(2);

        var report = backfillService.run(true, purchase.productId());

        assertThat(report.dryRun()).isTrue();
        assertThat(report.entitlementsPinned()).isEqualTo(1);
        assertThat(report.assetsPinned()).isEqualTo(2);

        assertThat(entitlementAssetRepo.countByEntitlement_Id(purchase.entitlementId()))
                .as("um ensaio nao pode gravar")
                .isZero();
    }

    @Test
    @DisplayName("a execucao real pina os arquivos das compras antigas")
    void realRunPins() {
        var purchase = legacyPurchaseWithFiles(2);

        var report = backfillService.run(false, purchase.productId());

        assertThat(report.entitlementsPinned()).isEqualTo(1);
        assertThat(report.assetsPinned()).isEqualTo(2);
        assertThat(report.entitlementsWithNoAssets()).isEmpty();

        assertThat(entitlementAssetRepo.countByEntitlement_Id(purchase.entitlementId())).isEqualTo(2);
    }

    @Test
    @DisplayName("rodar de novo pula quem ja foi pinado, sem mexer no que ja estava")
    void secondRunSkipsAlreadyPinned() {
        var purchase = legacyPurchaseWithFiles(2);

        backfillService.run(false, purchase.productId());
        var second = backfillService.run(false, purchase.productId());

        assertThat(second.entitlementsSkippedAlreadyPinned()).isEqualTo(1);
        assertThat(second.entitlementsPinned()).isZero();
        assertThat(entitlementAssetRepo.countByEntitlement_Id(purchase.entitlementId())).isEqualTo(2);
    }

    @Test
    @DisplayName("compra sem arquivo nenhum aparece na lista que bloqueia o cutover")
    void purchaseWithoutFilesIsReported() {
        var purchase = legacyPurchaseWithFiles(0);

        var report = backfillService.run(false, purchase.productId());

        assertThat(report.entitlementsPinned()).isZero();
        assertThat(report.entitlementsWithNoAssets())
                .as("enquanto esta lista nao estiver vazia, desligar a flag de "
                        + "compatibilidade tiraria o download de um comprador real")
                .hasSize(1)
                .allSatisfy(empty ->
                        assertThat(empty.entitlementId()).isEqualTo(purchase.entitlementId().toString()));
    }

    @Test
    @DisplayName("tambem pina as colecoes que acompanham toda oferta")
    void bundledCollectionsAreIncluded() {
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
        var entitlement = fixtures.entitlement(user, order, product, sold);

        var report = backfillService.run(false, product.getId());

        assertThat(report.assetsPinned()).isEqualTo(2);
        assertThat(entitlementAssetRepo.countByEntitlement_Id(entitlement.getId())).isEqualTo(2);
    }

    @Test
    @DisplayName("direito revogado fica de fora do backfill")
    void revokedEntitlementsAreIgnored() {
        var user = fixtures.user();
        var product = fixtures.product(TestDataFactory.unique("p"), "casas",
                TestDataFactory.unique("s"), ProductStatus.PUBLISHED);
        var collection = fixtures.collection(TestDataFactory.uniqueCode("COL"));
        var offer = fixtures.offer(product, collection, 150000);
        fixtures.asset(offer, "planta.pdf");

        var order = fixtures.order(user, OrderStatus.PAID, product, collection, 150000);
        var entitlement = fixtures.entitlement(user, order, product, collection);
        entitlement.setRevokedAt(java.time.Instant.now());

        var report = backfillService.run(false, product.getId());

        assertThat(report.entitlementsScanned()).isZero();
        assertThat(entitlementAssetRepo.countByEntitlement_Id(entitlement.getId())).isZero();
    }
}
