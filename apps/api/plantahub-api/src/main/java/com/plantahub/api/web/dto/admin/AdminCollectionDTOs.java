package com.plantahub.api.web.dto.admin;

import com.plantahub.api.domain.catalog.PlanType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class AdminCollectionDTOs {

    private AdminCollectionDTOs() {
    }

    public record CreateCollectionRequest(
            @NotBlank @Size(max = 40) String code,
            @NotBlank @Size(max = 120) String name,
            String description,
            Boolean purchasable,
            Boolean bundledWithEveryOffer,
            Integer sortOrder
    ) {}

    public record UpdateCollectionRequest(
            /** Presente apenas para detectar tentativa de renomear; alterar e recusado. */
            String code,
            @Size(max = 120) String name,
            String description,
            Boolean purchasable,
            Boolean bundledWithEveryOffer,
            Integer sortOrder
    ) {}

    public record CollectionDTO(
            UUID id,
            String code,
            String name,
            String description,
            boolean purchasable,
            boolean bundledWithEveryOffer,
            boolean active,
            int sortOrder,
            long assetCount
    ) {
        public static CollectionDTO from(PlanType c, long assetCount) {
            return new CollectionDTO(
                    c.getId(), c.getCode(), c.getName(), c.getDescription(),
                    Boolean.TRUE.equals(c.getPurchasable()),
                    Boolean.TRUE.equals(c.getBundledWithEveryOffer()),
                    Boolean.TRUE.equals(c.getActive()),
                    c.getSortOrder() == null ? 0 : c.getSortOrder(),
                    assetCount
            );
        }
    }
}
