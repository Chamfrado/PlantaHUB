package com.plantahub.api.domain.catalog;

import com.plantahub.api.domain.auth.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Um arquivo real que existe no bucket, pertencente a uma colecao de um produto.
 *
 * <p>{@link #storageKey} e a <b>unica</b> fonte da verdade sobre onde o arquivo esta.
 * Nenhum codigo pode remontar essa chave a partir de id de produto, codigo de colecao ou
 * nome de pasta: era exatamente isso que produzia {@code NoSuchKey} quando a convencao
 * assumida pelo codigo divergia do que estava no bucket.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "digital_asset")
public class DigitalAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_plan_type_id", nullable = false)
    private ProductPlanType productPlanType;

    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 1;

    /** Nome exibido ao cliente. Nao participa da resolucao do objeto no bucket. */
    @Column(name = "filename", nullable = false, length = 255)
    private String filename;

    /** Chave literal no bucket, exatamente como o objeto existe. Nunca reconstruida. */
    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @Column(name = "kind", nullable = false, length = 50)
    @Builder.Default
    private String kind = "FILE";

    @Column(name = "media_type", length = 120)
    private String mediaType;

    @Column(name = "file_ext", length = 20)
    private String fileExt;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    /** Caminho dentro da colecao, quando o arquivo veio de um upload de pasta. */
    @Column(name = "relative_path", length = 400)
    private String relativePath;

    /**
     * {@code LEGACY} = chave que ja existia no bucket, preservada literalmente pela
     * reconciliacao. {@code V2} = chave gerada no upload novo. O runtime nunca ramifica
     * neste campo; ele existe para a UI admin e para uma eventual limpeza futura.
     */
    @Column(name = "key_scheme", nullable = false, length = 20)
    @Builder.Default
    private String keyScheme = "LEGACY";

    @Column(name = "reconciliation_status", nullable = false, length = 30)
    @Builder.Default
    private String reconciliationStatus = "UNKNOWN";

    /**
     * Exclusao logica. Atencao: o caminho de download <b>nao</b> filtra por este campo —
     * um asset ja concedido a um comprador precisa continuar baixando mesmo depois de o
     * admin remove-lo do catalogo. Somente as visoes de admin e catalogo filtram.
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

    @Column(name = "etag", length = 100)
    private String etag;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
