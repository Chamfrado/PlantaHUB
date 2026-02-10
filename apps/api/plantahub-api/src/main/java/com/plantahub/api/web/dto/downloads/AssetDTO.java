package com.plantahub.api.web.dto.downloads;

public record AssetDTO(
        String kind,
        int version,
        String filename,
        String storageKey,
        String url
) {}
