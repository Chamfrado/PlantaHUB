package com.plantahub.api.service;

import com.plantahub.api.domain.downloads.DownloadEntitlement;
import com.plantahub.api.domain.orders.Order;
import com.plantahub.api.domain.orders.OrderItem;
import com.plantahub.api.domain.orders.OrderItemSelection;
import com.plantahub.api.domain.orders.enums.OrderStatus;
import com.plantahub.api.integration.infinitepay.dto.InfinitePayWebhookDTO;
import com.plantahub.api.repository.DownloadEntitlementRepository;
import com.plantahub.api.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class InfinitePayWebhookService {

    private final OrderRepository orderRepository;
    private final DownloadEntitlementRepository entitlementRepo;

    public InfinitePayWebhookService(
            OrderRepository orderRepository,
            DownloadEntitlementRepository entitlementRepo
    ) {
        this.orderRepository = orderRepository;
        this.entitlementRepo = entitlementRepo;
    }

    @Transactional
    public void handlePaidOrder(InfinitePayWebhookDTO payload) {
        if (payload.order_nsu() == null || payload.order_nsu().isBlank()) {
            throw new IllegalArgumentException("missing_order_nsu");
        }

        UUID orderId = UUID.fromString(payload.order_nsu());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("order_not_found"));

        if (order.getStatus() == OrderStatus.PAID) {
            return;
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("order_not_payable");
        }

        validateAmount(order, payload);

        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(Instant.now());
        order.setPaymentProvider("INFINITEPAY");
        order.setPaymentInvoiceSlug(payload.invoice_slug());
        order.setPaymentTransactionNsu(payload.transaction_nsu());
        order.setPaymentReceiptUrl(payload.receipt_url());
        order.setPaymentCaptureMethod(payload.capture_method());
        order.setPaymentPaidAmountCents(payload.paid_amount());

        grantEntitlementsFromOrder(order);

        orderRepository.save(order);
    }

    private void validateAmount(Order order, InfinitePayWebhookDTO payload) {
        if (payload.amount() == null) {
            throw new IllegalArgumentException("missing_payment_amount");
        }

        if (!payload.amount().equals(order.getTotalCents().longValue())) {
            throw new IllegalArgumentException("payment_amount_mismatch");
        }
    }

    private void grantEntitlementsFromOrder(Order order) {
        UUID userId = order.getUser().getId();

        for (OrderItem item : order.getItems()) {
            String productId = item.getProduct().getId();

            for (OrderItemSelection sel : item.getSelections()) {
                UUID planTypeId = sel.getPlanType().getId();

                if (entitlementRepo.existsByUserIdAndProductIdAndPlanTypeId(userId, productId, planTypeId)) {
                    continue;
                }

                DownloadEntitlement ent = new DownloadEntitlement();
                ent.setUser(order.getUser());
                ent.setOrder(order);
                ent.setProduct(item.getProduct());
                ent.setPlanType(sel.getPlanType());
                ent.setGrantedAt(Instant.now());

                entitlementRepo.save(ent);
            }
        }
    }
}