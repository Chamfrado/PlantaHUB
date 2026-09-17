package com.plantahub.api.domain.catalog;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Uma colecao de arquivos que pode ser oferecida num produto.
 *
 * <p>O nome da tabela ({@code plan_type}) e do tipo sao herdados de quando so existiam
 * cinco codigos fixos no codigo (ARCH, HYD, ELEC, STR, LAND). Hoje as linhas sao criadas
 * pelo admin e nada no sistema conhece codigo nenhum de antemao.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "plan_type")
public class PlanType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Identificador estavel da colecao, em maiusculas.
     *
     * <p><b>Imutavel depois de criado.</b> Este valor esta gravado dentro das chaves S3
     * legadas e e o que o mapeamento pasta&rarr;colecao usa na reconciliacao e no upload
     * de pastas. Renomea-lo orfanaria, em silencio, todo arquivo legado daquela pasta.
     */
    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    /**
     * Se aparece no seletor publico e pode ser comprada. Uma colecao de anexos
     * (ex.: documentos de apoio) e {@code false}: existe, tem arquivos, mas nao e oferta.
     */
    @Column(name = "purchasable", nullable = false)
    @Builder.Default
    private Boolean purchasable = true;

    /**
     * Se os arquivos desta colecao acompanham qualquer oferta comprada do mesmo produto.
     *
     * <p>E isto que substitui o literal {@code "APOIO"} que existia espalhado pelos
     * servicos de download. A uniao e resolvida uma unica vez, no momento do pagamento.
     */
    @Column(name = "bundled_with_every_offer", nullable = false)
    @Builder.Default
    private Boolean bundledWithEveryOffer = false;

    /** Permite ao admin esconder a colecao sem apagar nada. */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
