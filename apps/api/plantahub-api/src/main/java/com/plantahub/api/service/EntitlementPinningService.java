package com.plantahub.api.service;

import com.plantahub.api.domain.catalog.DigitalAsset;
import com.plantahub.api.domain.downloads.DownloadEntitlement;
import com.plantahub.api.domain.downloads.EntitlementAsset;
import com.plantahub.api.repository.DigitalAssetRepository;
import com.plantahub.api.repository.EntitlementAssetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Congela, no momento do pagamento, quais arquivos uma concessao da direito.
 *
 * <p>A uniao "colecao comprada + colecoes que acompanham toda oferta" e resolvida aqui,
 * uma unica vez. Isso tem duas consequencias deliberadas:
 *
 * <ul>
 *   <li>O caminho de leitura deixa de precisar conhecer a regra: baixar vira "leia
 *       entitlement_asset". Os literais espalhados pelos servicos de download somem.</li>
 *   <li>Editar o catalogo depois nao muda o que um comprador antigo recebe. O preco disso
 *       e que corrigir um arquivo tambem nao alcanca quem ja comprou — isso e uma acao
 *       administrativa explicita e auditada, nao um efeito colateral silencioso.</li>
 * </ul>
 */
@Service
public class EntitlementPinningService {

    private static final Logger log = LoggerFactory.getLogger(EntitlementPinningService.class);

    private final DigitalAssetRepository assetRepo;
    private final EntitlementAssetRepository entitlementAssetRepo;

    public EntitlementPinningService(DigitalAssetRepository assetRepo,
                                     EntitlementAssetRepository entitlementAssetRepo) {
        this.assetRepo = assetRepo;
        this.entitlementAssetRepo = entitlementAssetRepo;
    }

    /**
     * Pina os arquivos desta concessao, se ela ainda nao foi pinada.
     *
     * <p><b>Nao faz nada quando a concessao ja tem algum arquivo pinado.</b> Essa guarda e
     * o coracao da garantia: sem ela, chamar este metodo de novo depois de o admin subir
     * arquivos novos entregaria esses arquivos a um comprador antigo — exatamente a
     * retroatividade que a pinagem existe para eliminar. Deixar a regra aqui, e nao na
     * responsabilidade de cada chamador, e o que a torna dificil de violar por engano.
     *
     * <p>Uma concessao com <i>zero</i> arquivos pinados continua elegivel de proposito: e
     * o caso de quem comprou uma colecao que ainda nao tinha arquivo nenhum, e que
     * portanto nao recebeu o que pagou.
     *
     * <p>Reentregar arquivos corrigidos a compradores antigos e uma acao administrativa
     * explicita e auditada, nao um efeito colateral de reprocessar um pagamento.
     *
     * @return quantos vinculos foram criados agora
     */
    @Transactional
    public int pin(DownloadEntitlement entitlement) {
        if (entitlementAssetRepo.existsByEntitlement_Id(entitlement.getId())) {
            return 0;
        }

        String productId = entitlement.getProduct().getId();
        String collectionCode = entitlement.getPlanType().getCode();

        Set<UUID> alreadyPinned = new HashSet<>(
                entitlementAssetRepo.findPinnedAssetIds(entitlement.getId()));

        List<EntitlementAsset> toCreate = new ArrayList<>();

        // 1) A colecao que o cliente escolheu e pagou.
        for (DigitalAsset asset : assetRepo.findActiveByProductAndCollectionCode(productId, collectionCode)) {
            if (alreadyPinned.add(asset.getId())) {
                toCreate.add(link(entitlement, asset, EntitlementAsset.Source.PURCHASED));
            }
        }

        // 2) As colecoes que acompanham qualquer oferta deste produto.
        //    Processadas depois de proposito: se um arquivo estiver nas duas listas, ele
        //    fica registrado como PURCHASED, que e a origem mais especifica.
        for (DigitalAsset asset : assetRepo.findActiveBundledByProduct(productId)) {
            if (alreadyPinned.add(asset.getId())) {
                toCreate.add(link(entitlement, asset, EntitlementAsset.Source.BUNDLED));
            }
        }

        if (toCreate.isEmpty()) {
            return 0;
        }

        entitlementAssetRepo.saveAll(toCreate);

        log.debug("Pinados {} arquivos no entitlement {} ({} / {})",
                toCreate.size(), entitlement.getId(), productId, collectionCode);

        return toCreate.size();
    }

    /**
     * Quantos arquivos {@link #pin} criaria agora, sem gravar nada.
     *
     * <p>Existe para o ensaio (dry run) do backfill: contar e decidir se o cutover e
     * seguro nao pode ter efeito colateral.
     */
    @Transactional(readOnly = true)
    public int countPinnableAssets(DownloadEntitlement entitlement) {
        if (entitlementAssetRepo.existsByEntitlement_Id(entitlement.getId())) {
            return 0;
        }

        String productId = entitlement.getProduct().getId();

        Set<UUID> distinct = new HashSet<>();
        assetRepo.findActiveByProductAndCollectionCode(productId, entitlement.getPlanType().getCode())
                .forEach(asset -> distinct.add(asset.getId()));
        assetRepo.findActiveBundledByProduct(productId)
                .forEach(asset -> distinct.add(asset.getId()));

        return distinct.size();
    }

    private EntitlementAsset link(DownloadEntitlement entitlement,
                                  DigitalAsset asset,
                                  EntitlementAsset.Source source) {
        return EntitlementAsset.builder()
                .entitlement(entitlement)
                .digitalAsset(asset)
                .source(source)
                .build();
    }
}
