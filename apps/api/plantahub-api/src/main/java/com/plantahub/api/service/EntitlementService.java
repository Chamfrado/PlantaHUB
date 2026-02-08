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

    public EntitlementService(AppUserRepository userRepo,
                              OrderRepository orderRepo,
                              DownloadEntitlementRepository entitlementRepo) {
        this.userRepo = userRepo;
        this.orderRepo = orderRepo;
        this.entitlementRepo = entitlementRepo;
    }

    /**
     * Endpoint temporário (dev): marca pedido como PAID e gera entitlements.
     * Idempotente: se já existir, não cria duplicado.
     */
    @Transactional
    public MarkPaidResponse markOrderPaid(String email, UUID orderId) {
        var user = userRepo.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("user_not_found"));

        var order = orderRepo.findByIdAndUserIdWithItems(orderId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("order_not_found"));

        // marca como PAID (se já for, mantém)
        if (order.getStatus() != OrderStatus.PAID) {
            order.setStatus(OrderStatus.PAID);
            order.setPaidAt(Instant.now());
        }

        int created = 0;
        Instant now = Instant.now();

        for (var item : order.getItems()) {
            var product = item.getProduct();

            for (var sel : item.getSelections()) {
                var planType = sel.getPlanType();

                boolean exists = entitlementRepo
                        .findByUser_IdAndOrder_IdAndProduct_IdAndPlanType_Id(
                                user.getId(), order.getId(), product.getId(), planType.getId()
                        ).isPresent();

                if (exists) continue;

                entitlementRepo.save(
                        DownloadEntitlement.builder()
                                .user(user)
                                .order(order)
                                .product(product)
                                .planType(planType)
                                .grantedAt(now)
                                .revokedAt(null)
                                .build()
                );
                created++;
            }
        }

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

    @Transactional(readOnly = true)
    public void assertHasEntitlement(String email, String productId, String planTypeCode) {
        var user = userRepo.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("user_not_found"));

        boolean ok = entitlementRepo.findActiveByUserId(user.getId()).stream()
                .anyMatch(e ->
                        e.getProduct().getId().equals(productId) &&
                                e.getPlanType().getCode().equalsIgnoreCase(planTypeCode)
                );

        if (!ok) throw new IllegalArgumentException("no_entitlement");
    }



    @Transactional(readOnly = true)
    public DownloadEntitlement validateEntitlement(String email, String productId, String planTypeCode) {
        var user = userRepo.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("user_not_found"));

        // busca entitlement ativo do user para aquele produto + tipo
        return entitlementRepo.findActiveByUserId(user.getId()).stream()
                .filter(e -> e.getProduct().getId().equals(productId))
                .filter(e -> e.getPlanType().getCode().equalsIgnoreCase(planTypeCode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no_entitlement"));
    }
}
