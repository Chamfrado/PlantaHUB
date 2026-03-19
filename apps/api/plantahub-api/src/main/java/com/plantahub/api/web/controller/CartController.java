package com.plantahub.api.web.controller;

import com.plantahub.api.service.CartService;
import com.plantahub.api.web.dto.cart.AddCartItemRequest;
import com.plantahub.api.web.dto.cart.CartResponse;
import com.plantahub.api.web.dto.cart.ReplaceCartItemSelectionsRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/me/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse getCart(@AuthenticationPrincipal UserDetails user) {
        return cartService.getCart(user.getUsername());
    }

    @DeleteMapping
    public void clearCart(@AuthenticationPrincipal UserDetails user) {
        cartService.clearCart(user.getUsername());
    }

    @PostMapping("/items")
    public CartResponse addItem(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody AddCartItemRequest req
    ) {
        return cartService.addItem(user.getUsername(), req.productId(), req.planTypeCodes());
    }

    @PutMapping("/items/{itemId}")
    public CartResponse replaceSelections(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID itemId,
            @Valid @RequestBody ReplaceCartItemSelectionsRequest req
    ) {
        return cartService.replaceSelections(user.getUsername(), itemId, req.planTypeCodes());
    }

    @DeleteMapping("/items/{itemId}")
    public void removeItem(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID itemId
    ) {
        cartService.removeItem(user.getUsername(), itemId);
    }
}