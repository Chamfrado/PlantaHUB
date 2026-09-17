package com.plantahub.api.entitlements;

import com.plantahub.api.domain.catalog.enums.ProductStatus;
import com.plantahub.api.domain.orders.enums.OrderStatus;
import com.plantahub.api.repository.DownloadEntitlementRepository;
import com.plantahub.api.support.AbstractApiTest;
import com.plantahub.api.support.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bug de dinheiro: cliente estornado que recompra.
 *
 * <p>Eram dois problemas somados. A constraint {@code (user, product, plan)} era total,
 * entao um entitlement revogado continuava ocupando o indice; e o dedupe da concessao
 * (usado nos dois caminhos de grant) nao filtrava {@code revokedAt}, entao o sistema
 * concluia "esse cliente ja tem" e nao criava nada. Resultado: o cliente pagava de novo e
 * ficava sem os arquivos, em silencio. Indice e metodo precisavam mudar juntos.
 */
@Transactional
class RepurchaseAfterRefundTest extends AbstractApiTest {

    @Autowired private DownloadEntitlementRepository entitlementRepo;
    @Autowired private TestDataFactory fixtures;

    @Test
    @DisplayName("o dedupe ignora entitlements revogados")
    void dedupeIgnoresRevoked() {
        var user = fixtures.user();
        var product = fixtures.product(TestDataFactory.unique("p"), "casas", TestDataFactory.unique("s"), ProductStatus.PUBLISHED);
        var collection = fixtures.collection(TestDataFactory.uniqueCode("COL"));
        fixtures.offer(product, collection, 150000);

        var firstOrder = fixtures.order(user, OrderStatus.REFUNDED, product, collection, 150000);
        var revoked = fixtures.entitlement(user, firstOrder, product, collection);
        revoked.setRevokedAt(Instant.now());
        entitlementRepo.saveAndFlush(revoked);

        boolean blocked = entitlementRepo.existsByUser_IdAndProduct_IdAndPlanType_IdAndRevokedAtIsNull(
                user.getId(), product.getId(), collection.getId());

        assertThat(blocked)
                .as("um direito revogado nao pode bloquear uma nova compra")
                .isFalse();
    }

    @Test
    @DisplayName("um novo entitlement pode ser criado depois do estorno")
    void newEntitlementFitsAfterRefund() {
        var user = fixtures.user();
        var product = fixtures.product(TestDataFactory.unique("p"), "casas", TestDataFactory.unique("s"), ProductStatus.PUBLISHED);
        var collection = fixtures.collection(TestDataFactory.uniqueCode("COL"));
        fixtures.offer(product, collection, 150000);

        var firstOrder = fixtures.order(user, OrderStatus.REFUNDED, product, collection, 150000);
        var revoked = fixtures.entitlement(user, firstOrder, product, collection);
        revoked.setRevokedAt(Instant.now());
        entitlementRepo.saveAndFlush(revoked);

        // O indice unico agora e parcial (WHERE revoked_at IS NULL), entao esta insercao
        // e possivel. Com a constraint total da V5/V9 ela estourava.
        var secondOrder = fixtures.order(user, OrderStatus.PAID, product, collection, 150000);
        var regranted = fixtures.entitlement(user, secondOrder, product, collection);
        entitlementRepo.flush();

        assertThat(regranted.getId()).isNotNull();
        assertThat(regranted.getRevokedAt()).isNull();

        boolean nowBlocked = entitlementRepo.existsByUser_IdAndProduct_IdAndPlanType_IdAndRevokedAtIsNull(
                user.getId(), product.getId(), collection.getId());

        assertThat(nowBlocked)
                .as("com um direito ativo, o dedupe volta a bloquear")
                .isTrue();
    }
}
