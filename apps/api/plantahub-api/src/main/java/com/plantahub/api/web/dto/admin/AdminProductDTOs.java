package com.plantahub.api.web.dto.admin;

import com.plantahub.api.domain.catalog.Product;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AdminProductDTOs {

    private AdminProductDTOs() {
    }

    public record CreateProductRequest(
            /** Opcional. Sem ele, o id vira {@code categoria-slug}. */
            @Size(max = 80) String id,
            @NotBlank @Size(max = 40) String category,
            @Size(max = 120) String slug,
            @NotBlank @Size(max = 160) String name,
            @Size(max = 255) String shortDescription,
            @PositiveOrZero Integer areaM2,
            @PositiveOrZero Integer basePriceCents,
            String delivery,
            Boolean customizable
    ) {}

    public record UpdateProductRequest(
            @Size(max = 40) String category,
            @Size(max = 120) String slug,
            @Size(max = 160) String name,
            @Size(max = 255) String shortDescription,
            @PositiveOrZero Integer areaM2,
            @PositiveOrZero Integer basePriceCents,
            String delivery,
            Boolean customizable
    ) {}

    /** Visao de lista do painel. */
    public record AdminProductSummaryDTO(
            String id,
            String category,
            String slug,
            String name,
            String status,
            Integer basePriceCents,
            String heroImageUrl,
            Instant publishedAt,
            Instant updatedAt
    ) {
        /**
         * @param basePriceCents menor oferta a venda, derivada das ofertas. Nao vem de
         *                       {@code product.base_price_cents}: essa coluna e um cache
         *                       que ninguem atualiza desde que o preco passou a ser por
         *                       oferta, e mostrava R$ 0,00 no painel inteiro.
         */
        public static AdminProductSummaryDTO from(Product p, Integer basePriceCents) {
            return new AdminProductSummaryDTO(
                    p.getId(), p.getCategory(), p.getSlug(), p.getName(),
                    p.getStatus().name(), basePriceCents, p.getHeroImageUrl(),
                    p.getPublishedAt(), p.getUpdatedAt()
            );
        }
    }
}
