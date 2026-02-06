package com.plantahub.api.repository;

import com.plantahub.api.domain.orders.Order;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Query("""
    select distinct o
    from Order o
    left join fetch o.items i
    left join fetch i.selections s
    left join fetch s.planType pt
    left join fetch i.product p
    where o.id = :orderId and o.user.id = :userId
  """)
    Optional<Order> findByIdAndUserIdWithItems(@Param("orderId") UUID orderId, @Param("userId") UUID userId);

    @Query("""
    select distinct o
    from Order o
    left join fetch o.items i
    left join fetch i.selections s
    left join fetch s.planType pt
    left join fetch i.product p
    where o.user.id = :userId
    order by o.createdAt desc
  """)
    List<Order> findByUserIdWithItems(@Param("userId") UUID userId);
}
