package com.plantahub.api.integration.infinitepay.dto;

import java.util.List;
import java.util.Map;

public record InfinitePayWebhookDTO(
        String invoice_slug,
        Long amount,
        Long paid_amount,
        Integer installments,
        String capture_method,
        String transaction_nsu,
        String order_nsu,
        String receipt_url,
        List<Map<String, Object>> items
) {
}