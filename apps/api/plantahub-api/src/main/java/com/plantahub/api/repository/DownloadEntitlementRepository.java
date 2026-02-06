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
}
