package com.plantahub.api.web.dto.downloads;

import java.util.List;

public record DownloadResponseDTO(
        String productId,
        String planTypeCode,
        List<FileDTO> files
) {
    public record FileDTO(
            String filename,
            String storageKey,
            String url,
            Long sizeBytes
    ) {}
}