package com.plantahub.api.repository;

import com.plantahub.api.domain.catalog.PlanType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlanTypeRepository extends JpaRepository<PlanType, UUID> {
    Optional<PlanType> findByCode(String code);
}
