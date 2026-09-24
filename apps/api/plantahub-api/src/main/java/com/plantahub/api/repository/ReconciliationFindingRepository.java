package com.plantahub.api.repository;

import com.plantahub.api.domain.ops.ReconciliationFinding;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReconciliationFindingRepository extends JpaRepository<ReconciliationFinding, UUID> {

    Page<ReconciliationFinding> findByRun_Id(UUID runId, Pageable pageable);

    Page<ReconciliationFinding> findByRun_IdAndType(UUID runId, ReconciliationFinding.Type type, Pageable pageable);

    List<ReconciliationFinding> findByRun_IdOrderByTypeAsc(UUID runId);

    long countByRun_IdAndType(UUID runId, ReconciliationFinding.Type type);
}
