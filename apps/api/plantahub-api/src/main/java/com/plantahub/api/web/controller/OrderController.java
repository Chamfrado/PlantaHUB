package com.plantahub.api.web.controller;

import com.plantahub.api.service.CheckoutService;
import com.plantahub.api.web.dto.orders.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
public class OrderController {

    private final CheckoutService checkoutService;

    public OrderController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/orders")
    public OrderResponseDTO createOrder(
            @AuthenticationPrincipal Object principal,
            @Valid @RequestBody CreateOrderRequest req
    ) {
        String email = extractEmail(principal);
        return checkoutService.createOrder(email, req);
    }

    @GetMapping("/orders/{id}")
    public OrderResponseDTO getOrder(
            @AuthenticationPrincipal Object principal,
            @PathVariable UUID id
    ) {
        String email = extractEmail(principal);
        return checkoutService.getOrder(email, id);
    }

    @GetMapping("/me/orders")
    public List<OrderResponseDTO> myOrders(@AuthenticationPrincipal Object principal) {
        String email = extractEmail(principal);
        return checkoutService.myOrders(email);
    }

    private String extractEmail(Object principal) {
        if (principal == null) return "";
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
            return ud.getUsername();
        }
        return principal.toString();
    }
}
