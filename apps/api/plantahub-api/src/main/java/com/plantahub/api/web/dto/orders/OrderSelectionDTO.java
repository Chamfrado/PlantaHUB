package com.plantahub.api.web.dto.orders;

import java.util.UUID;

public record OrderSelectionDTO(
        UUID id,
        String planTypeCode,
        String planTypeName,
        Integer priceCents
) {}
