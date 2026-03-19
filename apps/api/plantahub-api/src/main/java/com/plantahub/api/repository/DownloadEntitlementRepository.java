package com.plantahub.api.repository;

import com.plantahub.api.domain.downloads.DownloadEntitlement;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DownloadEntitlementRepository extends JpaRepository<DownloadEntitlement, UUID> {

    @Query("""
    select e
    from DownloadEntitlement e
    join fetch e.product p
    join fetch e.planType pt
    where e.user.id = :userId
      and e.revokedAt is null
    order by e.grantedAt desc
  """)
    List<DownloadEntitlement> findActiveByUserId(@Param("userId") UUID userId);

    Optional<DownloadEntitlement> findByUser_IdAndOrder_IdAndProduct_IdAndPlanType_Id(
            UUID userId, UUID orderId, String productId, UUID planTypeId
    );

    boolean existsByUserIdAndProductIdAndPlanTypeId(UUID userId, String productId, UUID planTypeId);


    @Query("""
    select de
    from DownloadEntitlement de
    join fetch de.product p
    join fetch de.planType pt
    join fetch de.order o
    join de.user u
    where u.email = :email
      and de.revokedAt is null
      and o.status = com.plantahub.api.domain.orders.enums.OrderStatus.PAID
    order by p.category, p.slug, pt.code
""")
    List<DownloadEntitlement> findActiveLibraryByEmail(String email);

    List<DownloadEntitlement> findByOrderIdAndRevokedAtIsNull(UUID orderId);

    @Query("""
    select de
    from DownloadEntitlement de
    join fetch de.product p
    join fetch de.planType pt
    join de.user u
    where u.email = :email
      and p.id = :productId
      and pt.code = :planTypeCode
      and de.revokedAt is null
      and de.order.status = com.plantahub.api.domain.orders.enums.OrderStatus.PAID
""")
    Optional<DownloadEntitlement> findActiveByUserEmailAndProductIdAndPlanTypeCode(
            @Param("email") String email,
            @Param("productId") String productId,
            @Param("planTypeCode") String planTypeCode
    );
}
