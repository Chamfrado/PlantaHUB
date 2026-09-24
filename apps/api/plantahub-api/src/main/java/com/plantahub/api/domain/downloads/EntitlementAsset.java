package com.plantahub.api.domain.downloads;

import com.plantahub.api.domain.catalog.DigitalAsset;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Um arquivo especifico que uma compra deu direito, congelado no momento do pagamento.
 *
 * <p>E o que torna uma compra reproduzivel: o admin pode editar, reordenar ou remover
 * arquivos do catalogo depois, e quem ja comprou continua baixando exatamente o que
 * comprou. Antes disso, a lista era descoberta listando prefixos no S3 a cada download,
 * entao mexer no bucket reescrevia, retroativamente, o conteudo de compras antigas.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(
        name = "entitlement_asset",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_entitlement_asset",
                columnNames = {"entitlement_id", "digital_asset_id"}
        )
)
public class EntitlementAsset {

    /** Como o arquivo entrou nesta concessao. */
    public enum Source {
        /** Veio da colecao que o cliente escolheu e pagou. */
        PURCHASED,
        /** Veio de uma colecao marcada como {@code bundledWithEveryOffer}. */
        BUNDLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entitlement_id", nullable = false)
    private DownloadEntitlement entitlement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "digital_asset_id", nullable = false)
    private DigitalAsset digitalAsset;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private Source source;

    @Column(name = "pinned_at", nullable = false)
    private Instant pinnedAt;

    @PrePersist
    void onCreate() {
        if (pinnedAt == null) pinnedAt = Instant.now();
    }
}
