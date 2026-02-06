package com.plantahub.api.service;

import com.plantahub.api.domain.orders.*;
import com.plantahub.api.domain.orders.enums.OrderStatus;
import com.plantahub.api.repository.*;
import com.plantahub.api.web.dto.orders.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class CheckoutService {

    private final AppUserRepository userRepo;
    private final ProductRepository productRepo;
    private final ProductPlanTypeRepository pptRepo;
    private final PlanTypeRepository planTypeRepo;
    private final OrderRepository orderRepo;

    public CheckoutService(
            AppUserRepository userRepo,
            ProductRepository productRepo,
            ProductPlanTypeRepository pptRepo,
            PlanTypeRepository planTypeRepo,
            OrderRepository orderRepo
    ) {
        this.userRepo = userRepo;
        this.productRepo = productRepo;
        this.pptRepo = pptRepo;
        this.planTypeRepo = planTypeRepo;
        this.orderRepo = orderRepo;
    }

    @Transactional
    public OrderResponseDTO createOrder(String email, CreateOrderRequest req) {
        var user = userRepo.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("user_not_found"));

        var product = productRepo.findById(req.productId())
                .orElseThrow(() -> new IllegalArgumentException("product_not_found"));

        // tipos disponíveis do produto (com PlanType carregado)
        var available = pptRepo.findAvailableByProductIdWithPlanType(product.getId());

        Map<String, com.plantahub.api.domain.catalog.ProductPlanType> byCode = new HashMap<>();
        for (var ppt : available) {
            byCode.put(ppt.getPlanType().getCode(), ppt);
        }

        List<String> selectedCodes = req.selectedPlanTypes().stream()
                .map(String::toUpperCase)
                .distinct()
                .toList();

        if (selectedCodes.isEmpty()) throw new IllegalArgumentException("no_plan_types_selected");

        for (String code : selectedCodes) {
            if (!byCode.containsKey(code)) throw new IllegalArgumentException("invalid_plan_type: " + code);
        }

        Instant now = Instant.now();

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING) // seu enum default é PENDING
                .currency("BRL")
                .totalCents(0)
                .createdAt(now)
                .paidAt(null)
                .build();

        int base = Optional.ofNullable(product.getBasePriceCents()).orElse(0);
        int addons = 0;

        OrderItem item = OrderItem.builder()
                .order(order)
                .product(product)
                .quantity(1)
                .unitPriceCents(base)
                .totalCents(0)
                .build();

        for (String code : selectedCodes) {
            var ppt = byCode.get(code);

            int price = Optional.ofNullable(ppt.getPriceCents()).orElse(0);
            addons += price;

            // traduz code -> PlanType (id) pra salvar em order_item_selection
            var planType = ppt.getPlanType(); // já veio carregado pelo join fetch

            item.getSelections().add(
                    OrderItemSelection.builder()
                            .orderItem(item)
                            .planType(planType)
                            .priceCents(price)
                            .chosenFormat(null) // A
                            .build()
            );
        }

        int total = base + addons;
        item.setTotalCents(total);

        order.getItems().add(item);
        order.setTotalCents(total);

        Order saved = orderRepo.save(order);
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponseDTO getOrder(String email, UUID orderId) {
        var user = userRepo.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("user_not_found"));

        var order = orderRepo.findByIdAndUserIdWithItems(orderId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("order_not_found"));

        return toDTO(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> myOrders(String email) {
        var user = userRepo.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("user_not_found"));

        return orderRepo.findByUserIdWithItems(user.getId()).stream()
                .map(this::toDTO)
                .toList();
    }

    private OrderResponseDTO toDTO(Order o) {
        var items = o.getItems().stream().map(i ->
                new OrderItemDTO(
                        i.getId(),
                        i.getProduct().getId(),
                        i.getProduct().getName(),
                        i.getQuantity(),
                        i.getUnitPriceCents(),
                        i.getTotalCents(),
                        i.getSelections().stream().map(s ->
                                new OrderSelectionDTO(
                                        s.getId(),
                                        s.getPlanType().getCode(),
                                        s.getPlanType().getName(),
                                        s.getPriceCents()
                                )
                        ).toList()
                )
        ).toList();

        return new OrderResponseDTO(
                o.getId(),
                o.getStatus().name(),
                o.getCurrency(),
                o.getTotalCents(),
                items
        );
    }
}
