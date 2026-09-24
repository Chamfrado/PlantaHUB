package com.plantahub.api.service;

import com.plantahub.api.domain.downloads.DownloadEntitlement;
import com.plantahub.api.domain.orders.enums.OrderStatus;
import com.plantahub.api.repository.*;
import com.plantahub.api.web.dto.downloads.DownloadDTO;
import com.plantahub.api.web.dto.orders.MarkPaidResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.plantahub.api.domain.downloads.DownloadEntitlement;
import com.plantahub.api.repository.DownloadEntitlementRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class EntitlementService {

    private final AppUserRepository userRepo;
    private final OrderRepository orderRepo;
    private final DownloadEntitlementRepository entitlementRepo;
    private final EntitlementGrantService entitlementGrantService;

    public EntitlementService(AppUserRepository userRepo,
                              OrderRepository orderRepo,
                              DownloadEntitlementRepository entitlementRepo,
                              EntitlementGrantService entitlementGrantService) {
        this.userRepo = userRepo;
        this.orderRepo = orderRepo;
        this.entitlementRepo = entitlementRepo;
        this.entitlementGrantService = entitlementGrantService;
    }

    /**
     * Atalho de desenvolvimento: marca o pedido como PAID e concede os direitos.
     *
     * <p>Delega ao {@link EntitlementGrantService}. Antes tinha concessao propria, com uma
     * chave de deduplicacao diferente das outras duas — ela olhava tambem o pedido, entao
     * o mesmo cliente recomprando a mesma colecao num pedido novo estourava a constraint.
     */
    @Transactional
    public MarkPaidResponse markOrderPaid(String email, UUID orderId) {
        var user = userRepo.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("user_not_found"));

        var order = orderRepo.findByIdAndUserIdWithItems(orderId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("order_not_found"));

        if (order.getStatus() != OrderStatus.PAID) {
            order.setStatus(OrderStatus.PAID);
            order.setPaidAt(Instant.now());
        }

        int created = entitlementGrantService.grantForPaidOrder(order).size();

        return new MarkPaidResponse(order.getId(), order.getStatus().name(), created);
    }

    @Transactional(readOnly = true)
    public List<DownloadDTO> myDownloads(String email) {
        var user = userRepo.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("user_not_found"));

        return entitlementRepo.findActiveByUserId(user.getId()).stream()
                .map(e -> new DownloadDTO(
                        e.getProduct().getId(),
                        e.getProduct().getName(),
                        e.getProduct().getCategory(),
                        e.getProduct().getSlug(),
                        e.getPlanType().getCode(),
                        e.getPlanType().getName(),
                        e.getGrantedAt()
                ))
                .toList();
    }

    /**
     * Portao do download de um plano avulso.
     *
     * <p>Usa a query que exige {@code order.status = PAID}, a mesma do bundle e da
     * biblioteca. A versao anterior carregava todos os entitlements do usuario e
     * filtrava em memoria <b>sem</b> olhar o status do pedido — ou seja, um entitlement
     * pendurado num pedido nao pago rendia URL assinada de download.
     */
    @Transactional(readOnly = true)
    public DownloadEntitlement validateEntitlement(String email, String productId, String planTypeCode) {
        return entitlementRepo
                .findActiveByUserEmailAndProductIdAndPlanTypeCode(
                        email.toLowerCase(), productId, planTypeCode.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("no_entitlement"));
    }
}
