package com.plantahub.api.domain.orders;

import com.plantahub.api.domain.catalog.PlanType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(
        name = "order_item_selection",
        uniqueConstraints = @UniqueConstraint(name = "uk_ois_item_plan", columnNames = {"order_item_id", "plan_type_id"})
)
public class OrderItemSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_type_id", nullable = false)
    private PlanType planType;

    @Column(name = "price_cents", nullable = false)
    private Integer priceCents;

    @Column(name = "chosen_format")
    private String chosenFormat; // fica NULL (A)
}
