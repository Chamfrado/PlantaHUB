package com.plantahub.api.domain.catalog;

import com.plantahub.api.domain.catalog.content.ProductContent;
import com.plantahub.api.domain.catalog.enums.ProductStatus;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(
        name = "product",
        uniqueConstraints = @UniqueConstraint(name = "uk_product_category_slug", columnNames = {"category", "slug"})
)
public class Product {

    @Id
    @Column(length = 80)
    private String id; // ex: "casa-confort-80m2"

    @Column(nullable = false, length = 120)
    private String slug;

    @Column(nullable = false, length = 40)
    private String category; // "casas", "chales"

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "short_desc", nullable = false, length = 255)
    private String shortDesc;

    @Column(name = "hero_image_url")
    private String heroImageUrl;

    @Column(name = "area_m2", nullable = false)
    private Integer areaM2;

    @Column(name = "base_price_cents", nullable = false)
    private Integer basePriceCents;

    @Column(columnDefinition = "text")
    private String delivery;

    @Column(nullable = false)
    private Boolean customizable;

    /** Posicao na vitrine, dentro da categoria. Empates desempatam pelo nome. */
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    /**
     * @deprecated substituido por {@link #status}. Continua mapeado porque a coluna ainda
     * e NOT NULL no banco e producao roda com {@code ddl-auto=validate}; a coluna so cai
     * depois que nenhuma leitura depender dela. Use {@link #applyStatus(ProductStatus)}
     * para nao deixar os dois campos divergirem enquanto isso.
     */
    @Deprecated
    @Column(nullable = false)
    private Boolean active;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductStatus status;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    /**
     * Texto da pagina publica.
     *
     * <p>Sempre substitua o objeto inteiro em vez de mutar campos: o Hibernate detecta
     * mudanca em JSON comparando a serializacao, e o salvamento do admin e atomico.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private ProductContent content = ProductContent.empty();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Unico ponto que muda o status, para que {@code active} nao possa ficar
     * dessincronizado enquanto a coluna existir.
     */
    public void applyStatus(ProductStatus newStatus) {
        Instant now = Instant.now();

        this.status = newStatus;
        this.active = newStatus == ProductStatus.PUBLISHED;
        this.updatedAt = now;

        if (newStatus == ProductStatus.PUBLISHED && this.publishedAt == null) {
            this.publishedAt = now;
        }
        if (newStatus == ProductStatus.ARCHIVED) {
            this.archivedAt = now;
        }
    }

    public boolean isPublished() {
        return status == ProductStatus.PUBLISHED;
    }
}
