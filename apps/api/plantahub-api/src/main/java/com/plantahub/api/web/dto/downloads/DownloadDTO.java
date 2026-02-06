package com.plantahub.api.web.dto.downloads;

import java.time.Instant;

public record DownloadDTO(
        String productId,
        String productName,
        String category,
        String slug,
        String planTypeCode,
        String planTypeName,
        Instant grantedAt
) {}
