package com.plantahub.api.web.dto.admin;

import com.plantahub.api.domain.ops.ReconciliationFinding;
import com.plantahub.api.domain.ops.ReconciliationRun;

import java.time.Instant;
import java.util.UUID;

/** DTOs da superficie de reconciliacao. */
public final class ReconciliationDTOs {

    private ReconciliationDTOs() {
    }

    public record RunStartedDTO(UUID runId, String status) {}

    public record RunDTO(
            UUID id,
            boolean dryRun,
            String productId,
            String status,
            Instant startedAt,
            Instant finishedAt,
            int objectsScanned,
            int assetsCreated,
            int assetsUpdated,
            int assetsMatched,
            int findingsCount,
            String errorMessage
    ) {
        public static RunDTO from(ReconciliationRun run) {
            return new RunDTO(
                    run.getId(),
                    Boolean.TRUE.equals(run.getDryRun()),
                    run.getProductId(),
                    run.getStatus().name(),
                    run.getStartedAt(),
                    run.getFinishedAt(),
                    run.getObjectsScanned(),
                    run.getAssetsCreated(),
                    run.getAssetsUpdated(),
                    run.getAssetsMatched(),
                    run.getFindingsCount(),
                    run.getErrorMessage()
            );
        }
    }

    public record FindingDTO(
            UUID id,
            String type,
            String severity,
            String storageKey,
            String productId,
            String planTypeCode,
            UUID digitalAssetId,
            String detail
    ) {
        public static FindingDTO from(ReconciliationFinding finding) {
            return new FindingDTO(
                    finding.getId(),
                    finding.getType().name(),
                    finding.getSeverity().name(),
                    finding.getStorageKey(),
                    finding.getProductId(),
                    finding.getPlanTypeCode(),
                    finding.getDigitalAssetId(),
                    finding.getDetail()
            );
        }
    }
}
