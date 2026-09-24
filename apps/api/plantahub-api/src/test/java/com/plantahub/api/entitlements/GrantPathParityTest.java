package com.plantahub.api.entitlements;

import com.plantahub.api.domain.auth.AppUser;
import com.plantahub.api.domain.catalog.PlanType;
import com.plantahub.api.domain.catalog.Product;
import com.plantahub.api.domain.catalog.enums.ProductStatus;
import com.plantahub.api.domain.orders.Order;
import com.plantahub.api.domain.orders.enums.OrderStatus;
import com.plantahub.api.repository.DownloadEntitlementRepository;
import com.plantahub.api.repository.EntitlementAssetRepository;
import com.plantahub.api.service.EntitlementGrantService;
import com.plantahub.api.support.AbstractApiTest;
import com.plantahub.api.support.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Havia tres implementacoes de "pedido pago vira direito de download": duas identicas
 * byte a byte e uma terceira com chave de deduplicacao diferente. Agora ha uma so, e
 * todos os caminhos passam por ela.
 *
 * <p>Estes testes cobrem o comportamento dessa implementacao unica — o que antes era
 * impossivel de garantir sem testar tres copias e torcer para nao divergirem.
 */
@Transactional
class GrantPathParityTest extends AbstractApiTest {

    @Autowired private EntitlementGrantService grantService;
    @Autowired private DownloadEntitlementRepository entitlementRepo;
    @Autowired private EntitlementAssetRepository entitlementAssetRepo;
    @Autowired private TestDataFactory fixtures;

    private record Scenario(AppUser user, Product product, PlanType collection, Order order) {}

    private Scenario scenarioWithAssets() {
        var user = fixtures.user();
        var product = fixtures.product(TestDataFactory.unique("p"), "casas", TestDataFactory.unique("s"), ProductStatus.PUBLISHED);

        var sold = fixtures.collection(TestDataFactory.uniqueCode("SOLD"));
        var attachment = fixtures.collection(TestDataFactory.uniqueCode("ATT"), false, true);

        var soldOffer = fixtures.offer(product, sold, 150000);
        var attachmentOffer = fixtures.offer(product, attachment, 0, false);

        fixtures.asset(soldOffer, "planta.pdf");
        fixtures.asset(attachmentOffer, "apoio.pdf");

        var order = fixtures.order(user, OrderStatus.PAID, product, sold, 150000);
        return new Scenario(user, product, sold, order);
    }

    @Test
    @DisplayName("conceder um pedido pago cria o direito e ja pina os arquivos")
    void grantCreatesEntitlementAndPins() {
        var s = scenarioWithAssets();

        List<?> created = grantService.grantForPaidOrder(s.order());

        assertThat(created).hasSize(1);

        var entitlement = entitlementRepo
                .findActiveByUserEmailAndProductIdAndPlanTypeCode(
                        s.user().getEmail(), s.product().getId(), s.collection().getCode())
                .orElseThrow();

        // A colecao comprada mais a que acompanha toda oferta, resolvidas na compra.
        assertThat(entitlementAssetRepo.countByEntitlement_Id(entitlement.getId())).isEqualTo(2);
    }

    @Test
    @DisplayName("reprocessar o mesmo pedido nao duplica direito nem arquivo")
    void replayIsIdempotent() {
        var s = scenarioWithAssets();

        grantService.grantForPaidOrder(s.order());
        List<?> secondRun = grantService.grantForPaidOrder(s.order());

        assertThat(secondRun)
                .as("um webhook reentregue nao pode conceder de novo")
                .isEmpty();

        var entitlement = entitlementRepo
                .findActiveByUserEmailAndProductIdAndPlanTypeCode(
                        s.user().getEmail(), s.product().getId(), s.collection().getCode())
                .orElseThrow();

        assertThat(entitlementAssetRepo.countByEntitlement_Id(entitlement.getId())).isEqualTo(2);
    }

    @Test
    @DisplayName("reprocessar depois de arquivos novos nao os entrega ao comprador antigo")
    void replayAfterNewUploadsChangesNothing() {
        var s = scenarioWithAssets();

        grantService.grantForPaidOrder(s.order());

        var entitlement = entitlementRepo
                .findActiveByUserEmailAndProductIdAndPlanTypeCode(
                        s.user().getEmail(), s.product().getId(), s.collection().getCode())
                .orElseThrow();

        var offer = fixtures.offer(
                fixtures.product(TestDataFactory.unique("outro"), "casas", TestDataFactory.unique("s"), ProductStatus.PUBLISHED),
                fixtures.collection(TestDataFactory.uniqueCode("NEW")),
                1000);
        fixtures.asset(offer, "irrelevante.pdf");

        grantService.grantForPaidOrder(s.order());

        assertThat(entitlementAssetRepo.countByEntitlement_Id(entitlement.getId())).isEqualTo(2);
    }

    @Test
    @DisplayName("um segundo pedido da mesma colecao nao cria direito duplicado")
    void secondOrderOfSameCollectionIsDeduped() {
        var s = scenarioWithAssets();

        grantService.grantForPaidOrder(s.order());

        var secondOrder = fixtures.order(s.user(), OrderStatus.PAID, s.product(), s.collection(), 150000);
        List<?> created = grantService.grantForPaidOrder(secondOrder);

        assertThat(created)
                .as("o cliente ja tem direito ativo a esta colecao")
                .isEmpty();
    }
}
