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
  join fetch da.productPlanType ppt
  join fetch ppt.planType pt
  where ppt.product.id = :productId
    and pt.code = :planTypeCode
  order by da.kind asc, da.version desc
""")
    List<DigitalAsset> findAssetsForProductAndPlanType(
            @Param("productId") String productId,
            @Param("planTypeCode") String planTypeCode
    );

}
