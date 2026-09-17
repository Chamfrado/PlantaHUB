package com.plantahub.api.web.dto.admin;

import com.plantahub.api.domain.catalog.ProductPlanType;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;
import java.util.UUID;

public final class AdminOfferDTOs {

    private AdminOfferDTOs() {
    }

    public record OfferRequest(
            @PositiveOrZero Integer priceCents,
            Boolean available,
            Boolean includedInBundle,
            Integer sortOrder
    ) {}

    public record ReorderRequest(List<String> collectionCodes) {}

    public record OfferDTO(
            UUID id,
            String collectionCode,
            String collectionName,
            boolean purchasableCollection,
            boolean bundledWithEveryOffer,
            Integer priceCents,
            boolean available,
            boolean includedInBundle,
            int sortOrder
    ) {
        public static OfferDTO from(ProductPlanType offer) {
            var collection = offer.getPlanType();
            return new OfferDTO(
                    offer.getId(),
                    collection.getCode(),
                    collection.getName(),
                    Boolean.TRUE.equals(collection.getPurchasable()),
                    Boolean.TRUE.equals(collection.getBundledWithEveryOffer()),
                    offer.getPriceCents(),
                    Boolean.TRUE.equals(offer.getAvailable()),
                    Boolean.TRUE.equals(offer.getIncludedInBundle()),
                    offer.getSortOrder() == null ? 0 : offer.getSortOrder()
            );
        }
    }
}
