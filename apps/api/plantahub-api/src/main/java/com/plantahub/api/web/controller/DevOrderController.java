package com.plantahub.api.web.controller;

import com.plantahub.api.service.CheckoutService;
import com.plantahub.api.service.EntitlementService;
import com.plantahub.api.web.dto.orders.MarkPaidResponse;
import com.plantahub.api.web.dto.orders.OrderActionResponseDTO;
import com.plantahub.api.web.dto.orders.OrderResponseDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Atalhos de pagamento para desenvolvimento e teste.
 *
 * <p>Estes tres endpoints estavam no {@code OrderController} e no
 * {@code EntitlementController}, alcancaveis em producao por qualquer usuario
 * autenticado. Na pratica, qualquer cliente logado podia marcar o proprio pedido como
 * pago e receber os arquivos sem pagar nada.
 *
 * <p>{@code @Profile("!prod")} faz os handlers simplesmente nao existirem quando o perfil
 * {@code prod} esta ativo — nao ha rota para chamar, nao ha permissao para configurar
 * errado. O frontend esconde os botoes correspondentes no build de producao.
 */
@Profile("!prod")
@RestController
@RequestMapping("/v1")
public class DevOrderController {

    private final CheckoutService checkoutService;
    private final EntitlementService entitlementService;

    public DevOrderController(CheckoutService checkoutService, EntitlementService entitlementService) {
        this.checkoutService = checkoutService;
        this.entitlementService = entitlementService;
    }

    @PostMapping("/me/orders/{orderId}/pay-mock")
    public OrderResponseDTO payMock(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID orderId
    ) {
        return checkoutService.payMock(user.getUsername(), orderId);
    }

    @PostMapping("/me/orders/{orderId}/refund-mock")
    public OrderActionResponseDTO refundMock(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID orderId
    ) {
        return checkoutService.refundMock(user.getUsername(), orderId);
    }

    @PostMapping("/orders/{id}/mark-paid")
    public MarkPaidResponse markPaid(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable UUID id
    ) {
        return entitlementService.markOrderPaid(user.getUsername(), id);
    }
}
