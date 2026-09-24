package com.plantahub.api.repository;

import com.plantahub.api.domain.orders.Order;
import com.plantahub.api.domain.orders.enums.OrderStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.*;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    /** Existe algum pedido contendo este produto? Portao da exclusao fisica. */
    @Query("select count(oi) > 0 from OrderItem oi where oi.product.id = :productId")
    boolean existsByItemsProductId(@Param("productId") String productId);

    /** Alguem ja comprou esta colecao deste produto? Portao da remocao de oferta. */
    @Query("""
    select count(ois) > 0
    from OrderItemSelection ois
    where ois.orderItem.product.id = :productId
      and upper(ois.planType.code) = upper(:collectionCode)
  """)
    boolean existsBySelectionProductAndPlanType(@Param("productId") String productId,
                                                @Param("collectionCode") String collectionCode);


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

    @Query("""
    select distinct o from Order o
    left join fetch o.items i
    left join fetch i.selections s
    left join fetch s.planType pt
    left join fetch i.product p
    where o.user.email = :email
    order by o.createdAt desc
  """)
    List<Order> findMyOrders(@Param("email") String email);

    @Query("""
    select distinct o from Order o
    left join fetch o.items i
    left join fetch i.selections s
    left join fetch s.planType pt
    left join fetch i.product p
    where o.id = :orderId and o.user.email = :email
  """)
    Optional<Order> findByIdAndUserEmailWithItems(@Param("orderId") UUID orderId, @Param("email") String email);

    Optional<Order> findByIdAndUserEmail(UUID id, String email);

    List<Order> findByStatusAndCreatedAtLessThanEqual(OrderStatus status, Instant createdAt);
}
