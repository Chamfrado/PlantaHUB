package com.plantahub.api.web.dto.downloads;

public record DownloadBundleResponseDTO(
        String filename,
        String storageKey,
        String url,
        long expiresInSeconds
) {}