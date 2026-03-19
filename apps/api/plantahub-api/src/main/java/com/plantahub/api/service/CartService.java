package com.plantahub.api.service;

import com.plantahub.api.domain.cart.Cart;
import com.plantahub.api.domain.cart.CartItem;
import com.plantahub.api.domain.cart.CartItemSelection;
import com.plantahub.api.domain.cart.enums.CartStatus;
import com.plantahub.api.domain.catalog.PlanType;
import com.plantahub.api.domain.catalog.Product;
import com.plantahub.api.domain.catalog.ProductPlanType;
import com.plantahub.api.repository.*;
import com.plantahub.api.web.dto.cart.CartItemResponse;
import com.plantahub.api.web.dto.cart.CartPlanTypeResponse;
import com.plantahub.api.web.dto.cart.CartResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CartService {

    private static final String DEFAULT_CURRENCY = "BRL";

    private final CartRepository cartRepository;
    private final AppUserRepository userRepository;
    private final ProductRepository productRepository;
    private final PlanTypeRepository planTypeRepository;
    private final ProductPlanTypeRepository productPlanTypeRepository;
    private final CartItemSelectionRepository cartItemSelectionRepository;

    public CartService(
            CartRepository cartRepository,
            AppUserRepository userRepository,
            ProductRepository productRepository,
            PlanTypeRepository planTypeRepository,
            ProductPlanTypeRepository productPlanTypeRepository,
            CartItemSelectionRepository cartItemSelectionRepository
    ) {
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.planTypeRepository = planTypeRepository;
        this.productPlanTypeRepository = productPlanTypeRepository;
        this.cartItemSelectionRepository = cartItemSelectionRepository;
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(String email) {
        Cart cart = cartRepository.findDetailedByUserEmailAndStatus(email.toLowerCase(), CartStatus.ACTIVE)
                .orElse(null);

        if (cart == null) {
            return new CartResponse(null, CartStatus.ACTIVE.name(), List.of(), 0, DEFAULT_CURRENCY);
        }

        hydrateSelections(cart);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse addItem(String email, String productId, List<UUID> planTypeIds) {
        Cart cart = getOrCreateActiveCart(email);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("product_not_found"));

        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new IllegalArgumentException("product_inactive");
        }

        Set<UUID> uniquePlanTypeIds = normalizePlanTypeIds(planTypeIds);
        validateProductPlanTypes(product.getId(), uniquePlanTypeIds);

        CartItem item = cart.getItems().stream()
                .filter(existing -> existing.getProduct().getId().equals(productId))
                .findFirst()
                .orElseGet(() -> {
                    CartItem created = CartItem.builder()
                            .cart(cart)
                            .product(product)
                            .createdAt(Instant.now())
                            .build();
                    cart.getItems().add(created);
                    return created;
                });

        Set<UUID> existingSelectionIds = item.getSelections().stream()
                .map(selection -> selection.getPlanType().getId())
                .collect(Collectors.toSet());

        if (!uniquePlanTypeIds.equals(existingSelectionIds)) {
            item.getSelections().clear();

            Map<UUID, PlanType> planTypesById = planTypeRepository.findAllById(uniquePlanTypeIds).stream()
                    .collect(Collectors.toMap(PlanType::getId, Function.identity()));

            for (UUID planTypeId : uniquePlanTypeIds) {
                PlanType planType = planTypesById.get(planTypeId);

                if (planType == null) {
                    throw new IllegalArgumentException("plan_type_not_found");
                }

                item.getSelections().add(
                        CartItemSelection.builder()
                                .cartItem(item)
                                .planType(planType)
                                .build()
                );
            }
        }

        cart.touch();
        cartRepository.save(cart);

        Cart detailed = cartRepository.findDetailedByUserEmailAndStatus(email.toLowerCase(), CartStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("cart_not_found_after_save"));

        return toResponse(detailed);
    }

    @Transactional
    public CartResponse replaceSelections(String email, UUID itemId, List<UUID> planTypeIds) {
        Cart cart = cartRepository.findDetailedByUserEmailAndStatus(email.toLowerCase(), CartStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("cart_not_found"));

        CartItem item = cart.getItems().stream()
                .filter(existing -> existing.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("cart_item_not_found"));

        if (item.getCart().getStatus() != CartStatus.ACTIVE) {
            throw new IllegalArgumentException("cart_not_active");
        }

        Set<UUID> uniquePlanTypeIds = normalizePlanTypeIds(planTypeIds);
        validateProductPlanTypes(item.getProduct().getId(), uniquePlanTypeIds);

        item.getSelections().clear();

        Map<UUID, PlanType> planTypesById = planTypeRepository.findAllById(uniquePlanTypeIds).stream()
                .collect(Collectors.toMap(PlanType::getId, Function.identity()));

        for (UUID planTypeId : uniquePlanTypeIds) {
            PlanType planType = planTypesById.get(planTypeId);

            if (planType == null) {
                throw new IllegalArgumentException("plan_type_not_found");
            }

            item.getSelections().add(
                    CartItemSelection.builder()
                            .cartItem(item)
                            .planType(planType)
                            .build()
            );
        }

        item.getCart().touch();
        cartRepository.save(cart);

        Cart detailed = loadDetailedCartOrThrow(email);

        return toResponse(detailed);
    }

    @Transactional
    public void removeItem(String email, UUID itemId) {
        Cart cart = cartRepository.findDetailedByUserEmailAndStatus(email.toLowerCase(), CartStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("cart_not_found"));

        boolean removed = cart.getItems().removeIf(item -> item.getId().equals(itemId));
        if (!removed) {
            throw new IllegalArgumentException("cart_item_not_found");
        }

        cart.touch();
        cartRepository.save(cart);
    }

    @Transactional
    public void clearCart(String email) {
        Cart cart = cartRepository.findDetailedByUserEmailAndStatus(email.toLowerCase(), CartStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("cart_not_found"));

        cart.getItems().clear();
        cart.touch();
        cartRepository.save(cart);
    }

    private Cart getOrCreateActiveCart(String email) {
        return cartRepository.findByUser_EmailAndStatus(email.toLowerCase(), CartStatus.ACTIVE)
                .orElseGet(() -> {
                    var user = userRepository.findByEmailAndActiveTrueAndDeletedAtIsNull(email.toLowerCase())
                            .orElseThrow(() -> new IllegalArgumentException("user_not_found"));

                    Cart cart = Cart.builder()
                            .user(user)
                            .status(CartStatus.ACTIVE)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();

                    return cartRepository.save(cart);
                });
    }

    private Set<UUID> normalizePlanTypeIds(List<UUID> planTypeIds) {
        if (planTypeIds == null || planTypeIds.isEmpty()) {
            throw new IllegalArgumentException("cart_plan_types_required");
        }

        return planTypeIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void validateProductPlanTypes(String productId, Set<UUID> planTypeIds) {
        List<ProductPlanType> allowed = productPlanTypeRepository.findAllByProduct_Id(productId);

        Set<UUID> allowedPlanTypeIds = allowed.stream()
                .map(ppt -> ppt.getPlanType().getId())
                .collect(Collectors.toSet());

        for (UUID planTypeId : planTypeIds) {
            if (!allowedPlanTypeIds.contains(planTypeId)) {
                throw new IllegalArgumentException("invalid_plan_type_for_product");
            }
        }
    }

    private CartResponse toResponse(Cart cart) {
        Map<String, ProductPlanType> pricingByKey = loadPricingMap(cart);

        List<CartItemResponse> items = cart.getItems().stream()
                .sorted(Comparator.comparing(item -> item.getProduct().getSlug()))
                .map(item -> {
                    List<CartPlanTypeResponse> selections = item.getSelections().stream()
                            .sorted(Comparator.comparing(selection -> selection.getPlanType().getCode()))
                            .map(selection -> {
                                String key = pricingKey(item.getProduct().getId(), selection.getPlanType().getId());
                                ProductPlanType ppt = pricingByKey.get(key);

                                if (ppt == null) {
                                    throw new IllegalStateException("missing_price_for_cart_selection");
                                }

                                return new CartPlanTypeResponse(
                                        selection.getPlanType().getId(),
                                        selection.getPlanType().getCode(),
                                        selection.getPlanType().getName(),
                                        selection.getPlanType().getDescription(),
                                        ppt.getPriceCents()
                                );
                            })
                            .toList();

                    int itemTotal = selections.stream()
                            .mapToInt(CartPlanTypeResponse::priceCents)
                            .sum();

                    return new CartItemResponse(
                            item.getId(),
                            item.getProduct().getId(),
                            item.getProduct().getSlug(),
                            item.getProduct().getName(),
                            item.getProduct().getCategory(),
                            item.getProduct().getShortDesc(),
                            item.getProduct().getHeroImageUrl(),
                            selections,
                            itemTotal
                    );
                })
                .toList();

        int total = items.stream()
                .mapToInt(CartItemResponse::itemTotalCents)
                .sum();

        return new CartResponse(
                cart.getId(),
                cart.getStatus().name(),
                items,
                total,
                DEFAULT_CURRENCY
        );
    }

    private Map<String, ProductPlanType> loadPricingMap(Cart cart) {
        Set<String> productIds = cart.getItems().stream()
                .map(item -> item.getProduct().getId())
                .collect(Collectors.toSet());

        if (productIds.isEmpty()) {
            return Map.of();
        }

        return productPlanTypeRepository.findAllByProduct_IdIn(productIds).stream()
                .collect(Collectors.toMap(
                        ppt -> pricingKey(ppt.getProduct().getId(), ppt.getPlanType().getId()),
                        Function.identity()
                ));
    }

    private String pricingKey(String productId, UUID planTypeId) {
        return productId + ":" + planTypeId;
    }


    private Cart loadDetailedCartOrThrow(String email) {
        Cart cart = cartRepository.findDetailedByUserEmailAndStatus(email.toLowerCase(), CartStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("cart_not_found"));

        hydrateSelections(cart);
        return cart;
    }

    private void hydrateSelections(Cart cart) {
        if (cart.getItems().isEmpty()) {
            return;
        }

        List<UUID> itemIds = cart.getItems().stream()
                .map(CartItem::getId)
                .toList();

        List<CartItemSelection> selections = cartItemSelectionRepository.findAllByCartItemIdInWithPlanType(itemIds);

        Map<UUID, List<CartItemSelection>> selectionsByItemId = selections.stream()
                .collect(Collectors.groupingBy(selection -> selection.getCartItem().getId()));

        for (CartItem item : cart.getItems()) {
            item.getSelections().clear();
            item.getSelections().addAll(
                    selectionsByItemId.getOrDefault(item.getId(), List.of())
            );
        }
    }
}