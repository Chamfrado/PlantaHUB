package com.plantahub.api.domain.catalog;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(
        name = "product_plan_type",
        uniqueConstraints = @UniqueConstraint(name = "uk_ppt_product_plan", columnNames = {"product_id", "plan_type_id"})
)
public class ProductPlanType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_type_id", nullable = false)
    private PlanType planType;

    @Column(name = "price_cents")
    private Integer priceCents; // pode ser null

    @Column(name = "is_included_in_bundle", nullable = false)
    private Boolean includedInBundle;

    @Column(name = "is_available", nullable = false)
    private Boolean available;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
}
