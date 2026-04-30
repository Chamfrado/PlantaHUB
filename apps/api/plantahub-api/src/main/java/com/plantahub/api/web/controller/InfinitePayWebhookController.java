package com.plantahub.api.web.controller;

import com.plantahub.api.integration.infinitepay.dto.InfinitePayWebhookDTO;
import com.plantahub.api.service.InfinitePayWebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/webhooks/infinitepay")
public class InfinitePayWebhookController {

    private final InfinitePayWebhookService webhookService;

    public InfinitePayWebhookController(InfinitePayWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> receive(@RequestBody InfinitePayWebhookDTO payload) {
        webhookService.handlePaidOrder(payload);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "webhook_received"
        ));
    }
}