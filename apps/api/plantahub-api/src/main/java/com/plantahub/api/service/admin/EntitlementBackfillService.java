package com.plantahub.api.service.admin;

import com.plantahub.api.domain.downloads.DownloadEntitlement;
import com.plantahub.api.repository.DownloadEntitlementRepository;
import com.plantahub.api.repository.EntitlementAssetRepository;
import com.plantahub.api.service.EntitlementPinningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Pina as compras que ja existiam antes da pinagem existir.
 *
 * <p>Todo entitlement anterior a esta modernizacao aponta para um par (produto, colecao) e
 * nao para arquivos. Enquanto ele tiver zero arquivos pinados, o caminho de download
 * precisa cair no comportamento antigo (listagem por prefixo) para nao regredir — e por
 * isso a flag {@code app.downloads.legacy-fallback} existe.
 *
 * <p>O resultado mais importante deste servico e a lista
 * {@code entitlementsWithNoAssets}: enquanto ela nao estiver vazia, desligar aquela flag
 * deixaria compradores reais sem download. E o unico sinal confiavel de que o cutover e
 * seguro.
 */
@Service
public class EntitlementBackfillService {

    private static final Logger log = LoggerFactory.getLogger(EntitlementBackfillService.class);

    private final DownloadEntitlementRepository entitlementRepo;
    private final EntitlementAssetRepository entitlementAssetRepo;
    private final EntitlementPinningService pinningService;

    public EntitlementBackfillService(DownloadEntitlementRepository entitlementRepo,
                                      EntitlementAssetRepository entitlementAssetRepo,
                                      EntitlementPinningService pinningService) {
        this.entitlementRepo = entitlementRepo;
        this.entitlementAssetRepo = entitlementAssetRepo;
        this.pinningService = pinningService;
    }

    public record EmptyEntitlement(String entitlementId, String productId, String planTypeCode) {}

    public record Report(
            boolean dryRun,
            int entitlementsScanned,
            int entitlementsSkippedAlreadyPinned,
            int entitlementsPinned,
            int assetsPinned,
            List<EmptyEntitlement> entitlementsWithNoAssets
    ) {}

    /**
     * @param dryRun    quando verdadeiro, nada e gravado: apenas conta o que aconteceria
     * @param productId opcional, limita a um produto
     */
    @Transactional
    public Report run(boolean dryRun, String productId) {
        List<DownloadEntitlement> entitlements = productId != null && !productId.isBlank()
                ? entitlementRepo.findAllActiveByProductId(productId.trim())
                : entitlementRepo.findAllActive();

        int skipped = 0;
        int pinnedEntitlements = 0;
        int pinnedAssets = 0;
        List<EmptyEntitlement> empty = new ArrayList<>();

        for (DownloadEntitlement entitlement : entitlements) {
            // Pular quem ja tem pins nao e otimizacao: re-pinar entregaria arquivos novos
            // a um comprador antigo, que e exatamente o que a pinagem impede.
            if (entitlementAssetRepo.existsByEntitlement_Id(entitlement.getId())) {
                skipped++;
                continue;
            }

            int count = dryRun
                    ? pinningService.countPinnableAssets(entitlement)
                    : pinningService.pin(entitlement);

            if (count == 0) {
                empty.add(new EmptyEntitlement(
                        entitlement.getId().toString(),
                        entitlement.getProduct().getId(),
                        entitlement.getPlanType().getCode()));
                continue;
            }

            pinnedEntitlements++;
            pinnedAssets += count;
        }

        Report report = new Report(dryRun, entitlements.size(), skipped,
                pinnedEntitlements, pinnedAssets, empty);

        log.info("Backfill de pins (dryRun={}): {} analisados, {} ja pinados, {} pinados agora, "
                        + "{} arquivos, {} sem arquivo nenhum",
                dryRun, entitlements.size(), skipped, pinnedEntitlements, pinnedAssets, empty.size());

        return report;
    }
}
