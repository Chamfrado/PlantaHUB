package com.plantahub.api.domain.ops;

import com.plantahub.api.domain.auth.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** Uma varredura do bucket comparada ao que o banco conhece. */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "reconciliation_run")
public class ReconciliationRun {

    public enum Status { RUNNING, COMPLETED, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "started_by")
    private AppUser startedBy;

    /** Em dry run nada e escrito em {@code digital_asset}; so os achados sao registrados. */
    @Column(name = "dry_run", nullable = false)
    private Boolean dryRun;

    @Column(name = "product_id", length = 80)
    private String productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "objects_scanned", nullable = false)
    @Builder.Default
    private Integer objectsScanned = 0;

    @Column(name = "assets_created", nullable = false)
    @Builder.Default
    private Integer assetsCreated = 0;

    @Column(name = "assets_updated", nullable = false)
    @Builder.Default
    private Integer assetsUpdated = 0;

    @Column(name = "assets_matched", nullable = false)
    @Builder.Default
    private Integer assetsMatched = 0;

    @Column(name = "findings_count", nullable = false)
    @Builder.Default
    private Integer findingsCount = 0;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @PrePersist
    void onCreate() {
        if (startedAt == null) startedAt = Instant.now();
    }
}
