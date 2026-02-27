package com.plantahub.api.repository;

import com.plantahub.api.domain.catalog.DigitalAsset;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DigitalAssetRepository extends JpaRepository<DigitalAsset, UUID> {

    @Query("""
    select da
    from DigitalAsset da
    join da.productPlanType ppt
    join ppt.planType pt
    where ppt.product.id = :productId
      and pt.code = :planTypeCode
    order by da.createdAt desc
  """)
    List<DigitalAsset> findAllByProductAndPlanType(
            @Param("productId") String productId,
            @Param("planTypeCode") String planTypeCode
    );

}
