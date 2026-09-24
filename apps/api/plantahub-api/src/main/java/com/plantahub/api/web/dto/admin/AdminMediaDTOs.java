package com.plantahub.api.web.dto.admin;

import com.plantahub.api.domain.catalog.ProductMedia;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class AdminMediaDTOs {

    private AdminMediaDTOs() {
    }

    public record UpdateMediaRequest(
            ProductMedia.Role role,
            @Size(max = 255) String altText,
            Integer sortOrder
    ) {}

    public record ReorderRequest(List<UUID> mediaIds) {}

    public record MediaDTO(
            UUID id,
            String role,
            String storageKey,
            String url,
            String altText,
            int sortOrder,
            Long sizeBytes
    ) {
        public static MediaDTO from(ProductMedia m) {
            return new MediaDTO(
                    m.getId(),
                    m.getRole().name(),
                    m.getStorageKey(),
                    // Nulo quando a linha nao tem URL publica; o painel avisa em vez de
                    // renderizar uma chave de bucket como se fosse endereco.
                    m.getPublicUrl(),
                    m.getAltText(),
                    m.getSortOrder() == null ? 0 : m.getSortOrder(),
                    m.getSizeBytes()
            );
        }
    }
}
