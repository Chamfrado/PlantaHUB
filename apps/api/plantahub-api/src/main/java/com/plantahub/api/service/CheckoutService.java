package com.plantahub.api.service;

import com.plantahub.api.domain.auth.AppUser;
import com.plantahub.api.domain.orders.*;
import com.plantahub.api.domain.downloads.DownloadEntitlement;
import com.plantahub.api.domain.orders.enums.OrderStatus;
import com.plantahub.api.repository.*;
import com.plantahub.api.web.dto.orders.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class CheckoutService {

    private final AppUserRepository userRepo;
    private final OrderRepository orderRepo;
    private final ProductRepository productRepo;
    private final ProductPlanTypeRepository pptRepo;
    private final DownloadEntitlementRepository entitlementRepo;
    private final ProfileService profileService;

    public CheckoutService(
            AppUserRepository userRepo,
            OrderRepository orderRepo,
            ProductRepository productRepo,
            ProductPlanTypeRepository pptRepo,
            DownloadEntitlementRepository entitlementRepo,
            ProfileService profileService
    ) {
        this.userRepo = userRepo;
        this.orderRepo = orderRepo;
        this.productRepo = productRepo;
        this.pptRepo = pptRepo;
        this.entitlementRepo = entitlementRepo;
        this.profileService = profileService;
    }

    @Transactional
    public OrderResponseDTO createOrder(String email, CreateOrderRequest req) {
        profileService.assertProfileComplete(email);

        AppUser user = userRepo.findByEmail(email)
                .orElseThrow();

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setCurrency("BRL");
        order.setCreatedAt(Instant.now());

        int orderTotal = 0;

        for (var itemReq : req.items()) {
            var product = productRepo.findById(itemReq.productId())
                    .orElseThrow(() -> new IllegalArgumentException("product_not_found: " + itemReq.productId()));

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(itemReq.quantity());

            int selectionsTotal = 0;

            for (String codeRaw : itemReq.planTypeCodes()) {
                String code = codeRaw.toUpperCase();

                var ppt = pptRepo.findByProductIdAndPlanTypeCode(product.getId(), code)
                        .orElseThrow(() -> new IllegalArgumentException("plan_not_available: " + code));

                int price = ppt.getPriceCents();
                selectionsTotal += price;

                OrderItemSelection sel = new OrderItemSelection();
                sel.setOrderItem(item);
                sel.setPlanType(ppt.getPlanType());
                sel.setPriceCents(price);
                item.getSelections().add(sel);
            }

            int itemTotal = selectionsTotal * itemReq.quantity();
            item.setUnitPriceCents(selectionsTotal);
            item.setTotalCents(itemTotal);

            order.getItems().add(item);
            orderTotal += itemTotal;
        }

        order.setTotalCents(orderTotal);

        Order saved = orderRepo.save(order);
        return OrderMapper.toDto(saved);
    }

    @Transactional
    public OrderResponseDTO payMock(String email, UUID orderId) {
        profileService.assertProfileComplete(email);
        Order order = orderRepo.findByIdAndUserEmail(orderId, email)
                .orElseThrow(() -> new IllegalArgumentException("order_not_found"));

        if (order.getStatus() == OrderStatus.PAID) {
            return OrderMapper.toDto(order);
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("order_not_payable");
        }

        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(Instant.now());

        // 🔥 Gera entitlements baseado nas seleções (A)
        grantEntitlementsFromOrder(order);

        return OrderMapper.toDto(order);
    }

    private void grantEntitlementsFromOrder(Order order) {
        UUID userId = order.getUser().getId();

        for (OrderItem item : order.getItems()) {
            String productId = item.getProduct().getId();

            for (OrderItemSelection sel : item.getSelections()) {
                UUID planTypeId = sel.getPlanType().getId();

                // idempotente
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

    public List<OrderResponseDTO> myOrders(String email) {
        return orderRepo.findMyOrders(email).stream().map(OrderMapper::toDto).toList();
    }

    @Transactional
    public OrderActionResponseDTO cancelOrder(String email, UUID orderId) {
        Order order = orderRepo.findByIdAndUserEmail(orderId, email)
                .orElseThrow(() -> new IllegalArgumentException("order_not_found"));

        if (order.getStatus() == OrderStatus.CANCELED) {
            return new OrderActionResponseDTO(order.getId(), order.getStatus().name(), "order_already_canceled");
        }

        if (order.getStatus() == OrderStatus.REFUNDED) {
            throw new IllegalArgumentException("order_already_refunded");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("order_not_cancelable");
        }

        order.setStatus(OrderStatus.CANCELED);

        return new OrderActionResponseDTO(order.getId(), order.getStatus().name(), "order_canceled");
    }

    @Transactional
    public OrderActionResponseDTO refundMock(String email, UUID orderId) {
        Order order = orderRepo.findByIdAndUserEmail(orderId, email)
                .orElseThrow(() -> new IllegalArgumentException("order_not_found"));

        if (order.getStatus() == OrderStatus.REFUNDED) {
            return new OrderActionResponseDTO(order.getId(), order.getStatus().name(), "order_already_refunded");
        }

        if (order.getStatus() == OrderStatus.CANCELED) {
            throw new IllegalArgumentException("canceled_order_cannot_be_refunded");
        }

        if (order.getStatus() != OrderStatus.PAID) {
            throw new IllegalArgumentException("order_not_refundable");
        }

        order.setStatus(OrderStatus.REFUNDED);

        revokeEntitlementsFromOrder(order);

        return new OrderActionResponseDTO(order.getId(), order.getStatus().name(), "order_refunded");
    }

    private void revokeEntitlementsFromOrder(Order order) {
        var entitlements = entitlementRepo.findByOrderIdAndRevokedAtIsNull(order.getId());
        Instant now = Instant.now();

        for (var ent : entitlements) {
            ent.setRevokedAt(now);
        }
    }
}