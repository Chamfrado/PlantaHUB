package com.plantahub.api.domain.uploads;

import com.plantahub.api.domain.auth.AppUser;
import com.plantahub.api.domain.catalog.Product;
import com.plantahub.api.domain.catalog.ProductPlanType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Um upload que ja foi autorizado mas ainda nao foi confirmado.
 *
 * <p>Guarda a chave que o servidor escolheu antes de os bytes existirem. E isso que
 * impede a confirmacao de ser forjada para apontar a um objeto arbitrario do bucket.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "pending_upload")
public class PendingUpload {

    public enum TargetKind {
        /** Arquivo entregue ao cliente. */
        ASSET,
        /** Imagem de vitrine. Nunca vira arquivo comprável. */
        MEDIA,
        /**
         * Imagem citada pelo conteúdo da página (ex.: foto de um depoimento). Pública como
         * {@link #MEDIA}, mas não entra na galeria: quem guarda a URL é o próprio conteúdo.
         */
        CONTENT_IMAGE;

        /** Imagem pública, sob {@code public/}, com os limites de mídia. */
        public boolean isPublicImage() {
            return this != ASSET;
        }
    }

    public enum Status { PENDING, CONFIRMED, ABORTED, EXPIRED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private AppUser createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_kind", nullable = false, length = 20)
    private TargetKind targetKind;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_plan_type_id")
    private ProductPlanType productPlanType;

    /** Segmento aleatorio que torna a chave unica. Nao e o id do registro final. */
    @Column(name = "key_segment", nullable = false)
    private UUID keySegment;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "relative_path", length = 400)
    private String relativePath;

    /** Vem do cliente. Na confirmacao, o valor real e lido do bucket. */
    @Column(name = "declared_content_type", length = 120)
    private String declaredContentType;

    @Column(name = "declared_size_bytes")
    private Long declaredSizeBytes;

    @Column(name = "s3_multipart_upload_id", length = 255)
    private String multipartUploadId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public boolean isMultipart() {
        return multipartUploadId != null && !multipartUploadId.isBlank();
    }
}
