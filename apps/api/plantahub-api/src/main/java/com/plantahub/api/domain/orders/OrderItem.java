package com.plantahub.api.domain.orders;

import com.plantahub.api.domain.catalog.Product;
import jakarta.persistence.*;
import lombok.*;

import java.util.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "order_item")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price_cents", nullable = false)
    private Integer unitPriceCents; // base price

    @Column(name = "total_cents", nullable = false)
    private Integer totalCents; // base + addons

    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OrderItemSelection> selections = new HashSet<>();

}
