package com.plantahub.api.repository;

import com.plantahub.api.domain.catalog.ProductPlanType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductPlanTypeRepository extends JpaRepository<ProductPlanType, UUID> {

    @Query("""
    select ppt
    from ProductPlanType ppt
    join fetch ppt.planType pt
    where ppt.product.id = :productId
      and ppt.available = true
    order by ppt.sortOrder asc
  """)
    List<ProductPlanType> findAvailableByProductIdWithPlanType(String productId);

    Optional<ProductPlanType> findByProduct_IdAndPlanType_Code(String productId, String planTypeCode);

    List<ProductPlanType> findAllByProduct_Id(String productId);

    List<ProductPlanType> findAllByProduct_IdIn(Collection<String> productIds);
}
