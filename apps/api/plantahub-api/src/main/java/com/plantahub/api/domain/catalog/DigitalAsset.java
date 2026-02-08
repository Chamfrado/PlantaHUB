package com.plantahub.api.domain.catalog;

import com.plantahub.api.domain.catalog.enums.FileFormat;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(
        name = "digital_asset",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_asset_ppt_format_version",
                columnNames = {"product_plan_type_id", "format", "version"}
        )
)
public class DigitalAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_plan_type_id", nullable = false)
    private ProductPlanType productPlanType;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false, columnDefinition = "file_format")
    private FileFormat format;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "filename", nullable = false, length = 255)
    private String filename;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
