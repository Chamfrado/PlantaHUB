package com.plantahub.api.domain.ops;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/** Uma divergencia (ou uma acao tomada) durante uma varredura. */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "reconciliation_finding")
public class ReconciliationFinding {

    public enum Type {
        /** Objeto existia no bucket e nao tinha linha: linha criada com a chave literal. */
        CREATED,
        /** Linha existia com metadado faltando (tamanho, etag, tipo): preenchido. */
        METADATA_UPDATED,
        /** O produto nao tinha vinculo com essa colecao: vinculo criado. */
        MISSING_PRODUCT_PLAN_TYPE,
        /** A chave aponta para um produto que nao existe no banco. */
        UNKNOWN_PRODUCT,
        /** A pasta nao corresponde a nenhuma colecao cadastrada. */
        UNKNOWN_COLLECTION,
        /**
         * Imagem solta sob {@code products/{id}/}, provavelmente capa do produto.
         * Nunca vira arquivo comprável — sem esta regra, a capa entraria no download.
         */
        MEDIA_CANDIDATE,
        /** Chave que nao segue nenhum formato reconhecido. */
        UNPARSEABLE_KEY,
        /** Linha no banco cuja chave nao existe mais no bucket. Nunca apagada. */
        ORPHAN_DB,
        /** Dois arquivos vivos com o mesmo nome na mesma colecao. */
        DUPLICATE_FILENAME
    }

    public enum Severity { INFO, WARN, ERROR }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    private ReconciliationRun run;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 10)
    private Severity severity;

    @Column(name = "storage_key", length = 500)
    private String storageKey;

    @Column(name = "product_id", length = 80)
    private String productId;

    @Column(name = "plan_type_code", length = 40)
    private String planTypeCode;

    @Column(name = "digital_asset_id")
    private UUID digitalAssetId;

    @Column(name = "detail", columnDefinition = "text")
    private String detail;
}
