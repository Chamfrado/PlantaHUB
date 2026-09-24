package com.plantahub.api.entitlements;

import com.plantahub.api.domain.catalog.enums.ProductStatus;
import com.plantahub.api.domain.downloads.EntitlementAsset;
import com.plantahub.api.domain.orders.enums.OrderStatus;
import com.plantahub.api.repository.EntitlementAssetRepository;
import com.plantahub.api.service.EntitlementPinningService;
import com.plantahub.api.support.AbstractApiTest;
import com.plantahub.api.support.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * A pinagem e o que torna uma compra reproduzivel.
 *
 * <p>Tambem e onde a regra "esta colecao acompanha toda oferta" deixa de ser um literal no
 * codigo de download e vira uma consulta resolvida uma unica vez, na compra.
 */
@Transactional
class EntitlementPinningServiceTest extends AbstractApiTest {

    @Autowired private EntitlementPinningService pinningService;
    @Autowired private EntitlementAssetRepository entitlementAssetRepo;
    @Autowired private TestDataFactory fixtures;

    @Test
    @DisplayName("pina a colecao comprada e tambem as que acompanham toda oferta")
    void pinsPurchasedAndBundled() {
        var user = fixtures.user();
        var product = fixtures.product(TestDataFactory.unique("p"), "casas", TestDataFactory.unique("s"), ProductStatus.PUBLISHED);

        var sold = fixtures.collection(TestDataFactory.uniqueCode("SOLD"));
        var attachment = fixtures.collection(TestDataFactory.uniqueCode("ATT"), false, true);

        var soldOffer = fixtures.offer(product, sold, 150000);
        var attachmentOffer = fixtures.offer(product, attachment, 0, false);

        fixtures.asset(soldOffer, "planta-arquitetonica.pdf");
        fixtures.asset(soldOffer, "planta-arquitetonica.dwg");
        fixtures.asset(attachmentOffer, "memorial-descritivo.pdf");

        var order = fixtures.order(user, OrderStatus.PAID, product, sold, 150000);
        var entitlement = fixtures.entitlement(user, order, product, sold);

        int pinned = pinningService.pin(entitlement);

        assertThat(pinned).isEqualTo(3);

        assertThat(entitlementAssetRepo.findByEntitlement_Id(entitlement.getId()))
                .extracting(ea -> ea.getDigitalAsset().getFilename(), EntitlementAsset::getSource)
                .containsExactlyInAnyOrder(
                        tuple("planta-arquitetonica.pdf", EntitlementAsset.Source.PURCHASED),
                        tuple("planta-arquitetonica.dwg", EntitlementAsset.Source.PURCHASED),
                        tuple("memorial-descritivo.pdf", EntitlementAsset.Source.BUNDLED));
    }

    @Test
    @DisplayName("nao pina arquivos de colecoes que o cliente nao comprou")
    void doesNotPinOtherCollections() {
        var user = fixtures.user();
        var product = fixtures.product(TestDataFactory.unique("p"), "casas", TestDataFactory.unique("s"), ProductStatus.PUBLISHED);

        var bought = fixtures.collection(TestDataFactory.uniqueCode("BUY"));
        var notBought = fixtures.collection(TestDataFactory.uniqueCode("SKIP"));

        var boughtOffer = fixtures.offer(product, bought, 150000);
        var notBoughtOffer = fixtures.offer(product, notBought, 90000);

        fixtures.asset(boughtOffer, "comprado.pdf");
        fixtures.asset(notBoughtOffer, "nao-comprado.pdf");

        var order = fixtures.order(user, OrderStatus.PAID, product, bought, 150000);
        var entitlement = fixtures.entitlement(user, order, product, bought);

        pinningService.pin(entitlement);

        assertThat(entitlementAssetRepo.findByEntitlement_Id(entitlement.getId()))
                .extracting(ea -> ea.getDigitalAsset().getFilename())
                .containsExactly("comprado.pdf");
    }

    @Test
    @DisplayName("arquivo adicionado depois NAO entra numa concessao ja existente")
    void laterUploadsDoNotReachPastBuyers() {
        var user = fixtures.user();
        var product = fixtures.product(TestDataFactory.unique("p"), "casas", TestDataFactory.unique("s"), ProductStatus.PUBLISHED);

        var sold = fixtures.collection(TestDataFactory.uniqueCode("SOLD"));
        var attachment = fixtures.collection(TestDataFactory.uniqueCode("ATT"), false, true);

        var soldOffer = fixtures.offer(product, sold, 150000);
        var attachmentOffer = fixtures.offer(product, attachment, 0, false);

        fixtures.asset(soldOffer, "original.pdf");

        var order = fixtures.order(user, OrderStatus.PAID, product, sold, 150000);
        var entitlement = fixtures.entitlement(user, order, product, sold);

        pinningService.pin(entitlement);

        // O admin sobe arquivos depois da compra.
        fixtures.asset(soldOffer, "adicionado-depois.pdf");
        fixtures.asset(attachmentOffer, "apoio-adicionado-depois.pdf");

        // Chamar pin() de novo nao alcanca o comprador antigo: o servico e um no-op para
        // concessoes ja pinadas. E exatamente esta nao-retroatividade que a pinagem
        // existe para garantir.
        int pinnedAgain = pinningService.pin(entitlement);

        assertThat(pinnedAgain)
                .as("re-pinar nao pode acrescentar arquivos novos a uma compra antiga")
                .isZero();

        assertThat(entitlementAssetRepo.findByEntitlement_Id(entitlement.getId()))
                .extracting(ea -> ea.getDigitalAsset().getFilename())
                .containsExactly("original.pdf");
    }

    @Test
    @DisplayName("arquivo excluido logicamente nao e pinado em novas compras")
    void deletedAssetsAreNotPinned() {
        var user = fixtures.user();
        var product = fixtures.product(TestDataFactory.unique("p"), "casas", TestDataFactory.unique("s"), ProductStatus.PUBLISHED);

        var sold = fixtures.collection(TestDataFactory.uniqueCode("SOLD"));
        var offer = fixtures.offer(product, sold, 150000);

        fixtures.asset(offer, "vigente.pdf");
        var removed = fixtures.asset(offer, "removido.pdf");
        removed.setDeletedAt(java.time.Instant.now());

        var order = fixtures.order(user, OrderStatus.PAID, product, sold, 150000);
        var entitlement = fixtures.entitlement(user, order, product, sold);

        pinningService.pin(entitlement);

        assertThat(entitlementAssetRepo.findByEntitlement_Id(entitlement.getId()))
                .extracting(ea -> ea.getDigitalAsset().getFilename())
                .containsExactly("vigente.pdf");
    }

    @Test
    @DisplayName("quem comprou colecao vazia ainda recebe os arquivos quando eles chegam")
    void emptyEntitlementRemainsEligible() {
        var user = fixtures.user();
        var product = fixtures.product(TestDataFactory.unique("p"), "casas", TestDataFactory.unique("s"), ProductStatus.PUBLISHED);
        var sold = fixtures.collection(TestDataFactory.uniqueCode("SOLD"));
        var offer = fixtures.offer(product, sold, 150000);

        var order = fixtures.order(user, OrderStatus.PAID, product, sold, 150000);
        var entitlement = fixtures.entitlement(user, order, product, sold);

        // Comprou uma colecao que ainda nao tinha arquivo nenhum.
        assertThat(pinningService.pin(entitlement)).isZero();

        fixtures.asset(offer, "finalmente-publicado.pdf");

        // Zero pins significa "nao recebeu o que pagou", entao continua elegivel.
        assertThat(pinningService.pin(entitlement)).isEqualTo(1);
    }

    @Test
    @DisplayName("colecao sem arquivos pina zero sem estourar")
    void emptyCollectionPinsNothing() {
        var user = fixtures.user();
        var product = fixtures.product(TestDataFactory.unique("p"), "casas", TestDataFactory.unique("s"), ProductStatus.PUBLISHED);
        var sold = fixtures.collection(TestDataFactory.uniqueCode("SOLD"));
        fixtures.offer(product, sold, 150000);

        var order = fixtures.order(user, OrderStatus.PAID, product, sold, 150000);
        var entitlement = fixtures.entitlement(user, order, product, sold);

        assertThat(pinningService.pin(entitlement)).isZero();
    }
}
