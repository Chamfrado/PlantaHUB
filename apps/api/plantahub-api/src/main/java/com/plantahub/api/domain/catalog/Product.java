package com.plantahub.api.domain.catalog;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(
        name = "product",
        uniqueConstraints = @UniqueConstraint(name = "uk_product_category_slug", columnNames = {"category", "slug"})
)
public class Product {

    @Id
    @Column(length = 80)
    private String id; // ex: "casa-confort-80m2"

    @Column(nullable = false, length = 120)
    private String slug;

    @Column(nullable = false, length = 40)
    private String category; // "casas", "chales"

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "short_desc", nullable = false, length = 255)
    private String shortDesc;

    @Column(name = "hero_image_url")
    private String heroImageUrl;

    @Column(name = "area_m2", nullable = false)
    private Integer areaM2;

    @Column(name = "base_price_cents", nullable = false)
    private Integer basePriceCents;

    @Column(columnDefinition = "text")
    private String delivery;

    @Column(nullable = false)
    private Boolean customizable;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
