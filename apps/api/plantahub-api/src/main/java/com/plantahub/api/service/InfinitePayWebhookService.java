package com.plantahub.api.service;

import com.plantahub.api.domain.orders.Order;
import com.plantahub.api.domain.orders.enums.OrderStatus;
import com.plantahub.api.integration.infinitepay.dto.InfinitePayWebhookDTO;
import com.plantahub.api.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class InfinitePayWebhookService {

    private final OrderRepository orderRepository;
    private final EntitlementGrantService entitlementGrantService;

    public InfinitePayWebhookService(
            OrderRepository orderRepository,
            EntitlementGrantService entitlementGrantService
    ) {
        this.orderRepository = orderRepository;
        this.entitlementGrantService = entitlementGrantService;
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

        entitlementGrantService.grantForPaidOrder(order);

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
}