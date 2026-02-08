package com.plantahub.api.domain.orders;

import com.plantahub.api.domain.auth.AppUser;
import com.plantahub.api.domain.orders.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    private OrderStatus status;



    @Column(name = "total_cents", nullable = false)
    private Integer totalCents;

    @Column(name = "currency", nullable = false, columnDefinition = "CHAR(3)")
    private String currency;


    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();
}
