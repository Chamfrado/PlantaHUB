package com.plantahub.api.web.dto.orders;

import java.util.UUID;

public record MarkPaidResponse(
        UUID orderId,
        String status,
        int entitlementsCreated
) {}
