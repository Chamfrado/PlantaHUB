package com.plantahub.api.domain.cart;

import com.plantahub.api.domain.catalog.PlanType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "cart_item_selection",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_cart_item_selection_unique", columnNames = {"cart_item_id", "plan_type_id"})
        }
)
public class CartItemSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_item_id", nullable = false)
    private CartItem cartItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_type_id", nullable = false)
    private PlanType planType;
}