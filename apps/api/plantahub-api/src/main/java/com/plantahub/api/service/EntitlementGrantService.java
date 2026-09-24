package com.plantahub.api.service;

import com.plantahub.api.domain.downloads.DownloadEntitlement;
import com.plantahub.api.domain.orders.Order;
import com.plantahub.api.domain.orders.OrderItem;
import com.plantahub.api.domain.orders.OrderItemSelection;
import com.plantahub.api.repository.DownloadEntitlementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * O unico lugar que transforma um pedido pago em direitos de download.
 *
 * <p>Existiam tres implementacoes disto: duas identicas byte a byte (no checkout e no
 * webhook) e uma terceira com uma chave de deduplicacao diferente, que estourava a
 * constraint quando o mesmo cliente recomprava a mesma colecao num pedido novo. Com a
 * pinagem entrando em cena, manter tres copias significaria tres lugares para esquecer
 * de pinar.
 */
@Service
public class EntitlementGrantService {

    private final DownloadEntitlementRepository entitlementRepo;
    private final EntitlementPinningService pinningService;

    public EntitlementGrantService(DownloadEntitlementRepository entitlementRepo,
                                   EntitlementPinningService pinningService) {
        this.entitlementRepo = entitlementRepo;
        this.pinningService = pinningService;
    }

    /**
     * Concede — e pina — os direitos de um pedido pago.
     *
     * <p>Idempotente: se o cliente ja tem um direito ativo para aquele (produto, colecao),
     * nada e criado. Reprocessar um webhook nao duplica nem altera concessoes anteriores.
     *
     * @return as concessoes criadas agora
     */
    @Transactional
    public List<DownloadEntitlement> grantForPaidOrder(Order order) {
        UUID userId = order.getUser().getId();
        List<DownloadEntitlement> created = new ArrayList<>();

        for (OrderItem item : order.getItems()) {
            String productId = item.getProduct().getId();

            for (OrderItemSelection selection : item.getSelections()) {
                UUID planTypeId = selection.getPlanType().getId();

                // Direito revogado nao conta: um cliente estornado precisa poder recomprar.
                if (entitlementRepo.existsByUser_IdAndProduct_IdAndPlanType_IdAndRevokedAtIsNull(
                        userId, productId, planTypeId)) {
                    continue;
                }

                DownloadEntitlement entitlement = entitlementRepo.save(
                        DownloadEntitlement.builder()
                                .user(order.getUser())
                                .order(order)
                                .product(item.getProduct())
                                .planType(selection.getPlanType())
                                .grantedAt(Instant.now())
                                .build()
                );

                // Congela agora a lista de arquivos desta compra.
                pinningService.pin(entitlement);

                created.add(entitlement);
            }
        }

        return created;
    }
}
