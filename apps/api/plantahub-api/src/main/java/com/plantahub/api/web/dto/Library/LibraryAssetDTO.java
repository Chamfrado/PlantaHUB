package com.plantahub.api.web.dto.Library;

import java.time.Instant;

public record LibraryAssetDTO(
        String id,
        String filename,
        String storageKey,
        Integer version,
        Long sizeBytes,
        Instant createdAt
) {}