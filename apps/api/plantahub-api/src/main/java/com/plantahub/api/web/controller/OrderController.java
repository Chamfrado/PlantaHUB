package com.plantahub.api.web.controller;

import com.plantahub.api.service.CheckoutService;
import com.plantahub.api.web.dto.orders.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/v1")
public class OrderController {

    private final CheckoutService checkoutService;

    public OrderController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/me/orders")
    public OrderResponseDTO create(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody CreateOrderRequest req
    ) {
        return checkoutService.createOrder(user.getUsername(), req);
    }

    @PostMapping("/me/orders/{orderId}/pay-mock")
    public OrderResponseDTO payMock(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID orderId
    ) {
        return checkoutService.payMock(user.getUsername(), orderId);
    }

    @GetMapping("/me/orders")
    public List<OrderResponseDTO> myOrders(@AuthenticationPrincipal UserDetails user) {
        return checkoutService.myOrders(user.getUsername());
    }

    @PostMapping("/me/orders/{orderId}/cancel")
    public OrderActionResponseDTO cancel(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID orderId
    ) {
        return checkoutService.cancelOrder(user.getUsername(), orderId);
    }

    @PostMapping("/me/orders/{orderId}/refund-mock")
    public OrderActionResponseDTO refundMock(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID orderId
    ) {
        return checkoutService.refundMock(user.getUsername(), orderId);
    }

    @PostMapping("/me/checkout/from-cart")
    public OrderResponseDTO checkoutFromCart(@AuthenticationPrincipal UserDetails user) {
        return checkoutService.createOrderFromCart(user.getUsername());
    }
}