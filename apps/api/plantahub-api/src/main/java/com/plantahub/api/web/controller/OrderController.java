package com.plantahub.api.web.controller;

import com.plantahub.api.service.CheckoutService;
import com.plantahub.api.service.EntitlementService;
import com.plantahub.api.web.dto.orders.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
public class OrderController {

    private final CheckoutService checkoutService;
    private final EntitlementService entitlementService;

    public OrderController(CheckoutService checkoutService, EntitlementService entitlementService) {
        this.checkoutService = checkoutService;
        this.entitlementService = entitlementService;
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
        if (principal instanceof UserDetails ud) {
            return ud.getUsername();
        }
        return principal.toString();
    }

    //USO DE DEV SOMENTE!
    @PostMapping("/orders/mark/{id}")
    public ResponseEntity markAsPaid(@AuthenticationPrincipal Object principal,
                                     @PathVariable UUID id){


        String email = extractEmail(principal);
        entitlementService.markOrderPaid(email, id);
        return ResponseEntity.ok("marcado como ok");

    }



}
