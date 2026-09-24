package com.plantahub.api.web.dto.admin;

import com.plantahub.api.domain.catalog.DigitalAsset;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AdminAssetDTOs {

    private AdminAssetDTOs() {
    }

    public record UpdateAssetRequest(
            @Size(max = 255) String filename,
            @Size(max = 50) String kind,
            Integer sortOrder,
            @Size(max = 400) String relativePath
    ) {}

    public record MoveAssetRequest(@NotBlank String collectionCode) {}

    public record ReorderRequest(List<UUID> assetIds) {}

    public record AssetDTO(
            UUID id,
            String collectionCode,
            String filename,
            /** Exposta ao admin de proposito: e o que permite conferir contra o bucket. */
            String storageKey,
            String relativePath,
            String fileExt,
            Long sizeBytes,
            int sortOrder,
            String keyScheme,
            String reconciliationStatus,
            boolean deleted,
            Instant createdAt
    ) {
        public static AssetDTO from(DigitalAsset a) {
            return new AssetDTO(
                    a.getId(),
                    a.getProductPlanType().getPlanType().getCode(),
                    a.getFilename(),
                    a.getStorageKey(),
                    a.getRelativePath(),
                    a.getFileExt(),
                    a.getSizeBytes(),
                    a.getSortOrder() == null ? 0 : a.getSortOrder(),
                    a.getKeyScheme(),
                    a.getReconciliationStatus(),
                    a.getDeletedAt() != null,
                    a.getCreatedAt()
            );
        }
    }
}
