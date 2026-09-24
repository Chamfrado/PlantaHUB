package com.plantahub.api.service.admin;

import com.plantahub.api.domain.catalog.DigitalAsset;
import com.plantahub.api.domain.catalog.ProductPlanType;
import com.plantahub.api.repository.DigitalAssetRepository;
import com.plantahub.api.repository.EntitlementAssetRepository;
import com.plantahub.api.repository.ProductPlanTypeRepository;
import com.plantahub.api.repository.ProductRepository;
import com.plantahub.api.shared.exception.ConflictException;
import com.plantahub.api.shared.exception.NotFoundException;
import com.plantahub.api.web.dto.admin.AdminAssetDTOs.UpdateAssetRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Gerencia os arquivos ja existentes de um produto.
 *
 * <p>Nenhuma operacao aqui toca o bucket. {@code storage_key} e imutavel: mover objetos
 * invalidaria URLs assinadas ja emitidas e nao traria beneficio nenhum. Associar,
 * desassociar e reordenar sao todas mudancas de linha.
 */
@Service
public class AdminAssetService {

    private final ProductRepository productRepo;
    private final ProductPlanTypeRepository offerRepo;
    private final DigitalAssetRepository assetRepo;
    private final EntitlementAssetRepository pinRepo;

    public AdminAssetService(ProductRepository productRepo,
                             ProductPlanTypeRepository offerRepo,
                             DigitalAssetRepository assetRepo,
                             EntitlementAssetRepository pinRepo) {
        this.productRepo = productRepo;
        this.offerRepo = offerRepo;
        this.assetRepo = assetRepo;
        this.pinRepo = pinRepo;
    }

    @Transactional(readOnly = true)
    public List<DigitalAsset> list(String productId, String collectionCode, boolean includeDeleted) {
        if (!productRepo.existsById(productId)) {
            throw new NotFoundException("product_not_found");
        }

        return assetRepo.findByProductId(productId).stream()
                .filter(a -> includeDeleted || a.getDeletedAt() == null)
                .filter(a -> collectionCode == null || collectionCode.isBlank()
                        || a.getProductPlanType().getPlanType().getCode()
                            .equalsIgnoreCase(collectionCode.trim()))
                .sorted((a, b) -> {
                    int byCollection = a.getProductPlanType().getPlanType().getCode()
                            .compareTo(b.getProductPlanType().getPlanType().getCode());
                    if (byCollection != 0) return byCollection;

                    int bySort = Integer.compare(
                            a.getSortOrder() == null ? 0 : a.getSortOrder(),
                            b.getSortOrder() == null ? 0 : b.getSortOrder());
                    return bySort != 0 ? bySort : a.getFilename().compareTo(b.getFilename());
                })
                .toList();
    }

    @Transactional
    public DigitalAsset update(UUID assetId, UpdateAssetRequest request) {
        DigitalAsset asset = require(assetId);

        // filename e rotulo de exibicao: nao participa da resolucao do objeto no bucket.
        if (request.filename() != null && !request.filename().isBlank()) {
            asset.setFilename(request.filename().trim());
        }
        if (request.kind() != null) asset.setKind(request.kind());
        if (request.sortOrder() != null) asset.setSortOrder(request.sortOrder());
        if (request.relativePath() != null) asset.setRelativePath(request.relativePath());

        return assetRepo.save(asset);
    }

    /**
     * Move o arquivo para outra colecao do mesmo produto.
     *
     * <p>E a primitiva de associar e desassociar: o que muda e o vinculo, nunca a chave.
     * Restrito ao mesmo produto porque mover entre produtos mudaria quem tem direito a ele.
     */
    @Transactional
    public DigitalAsset move(UUID assetId, String collectionCode) {
        DigitalAsset asset = require(assetId);

        String productId = asset.getProductPlanType().getProduct().getId();
        String code = collectionCode.trim().toUpperCase(Locale.ROOT);

        ProductPlanType target = offerRepo.findByProduct_IdAndPlanType_Code(productId, code)
                .orElseThrow(() -> new NotFoundException(
                        "offer_not_found: crie o vinculo do produto com a colecao " + code));

        asset.setProductPlanType(target);
        return assetRepo.save(asset);
    }

    @Transactional
    public void reorder(String productId, List<UUID> assetIds) {
        if (!productRepo.existsById(productId)) {
            throw new NotFoundException("product_not_found");
        }

        int order = 0;

        for (UUID assetId : assetIds) {
            DigitalAsset asset = require(assetId);

            if (!asset.getProductPlanType().getProduct().getId().equals(productId)) {
                throw new ConflictException("asset_belongs_to_another_product");
            }

            asset.setSortOrder(order++);
            assetRepo.save(asset);
        }
    }

    /**
     * Exclusao logica.
     *
     * <p>Um arquivo ja concedido a algum comprador exige {@code force}, para que remover
     * algo do catalogo nunca seja confundido com "isso nao afeta ninguem". Mesmo forcada, a
     * exclusao e apenas logica: o caminho de download ignora {@code deletedAt}, entao quem
     * ja pagou continua baixando.
     */
    @Transactional
    public void delete(UUID assetId, boolean force) {
        DigitalAsset asset = require(assetId);

        if (!force && pinRepo.existsByDigitalAsset_Id(assetId)) {
            throw new ConflictException("asset_granted_to_customers_use_force");
        }

        asset.setDeletedAt(Instant.now());
        assetRepo.save(asset);
    }

    @Transactional
    public DigitalAsset restore(UUID assetId) {
        DigitalAsset asset = require(assetId);
        asset.setDeletedAt(null);
        return assetRepo.save(asset);
    }

    private DigitalAsset require(UUID assetId) {
        return assetRepo.findById(assetId)
                .orElseThrow(() -> new NotFoundException("asset_not_found"));
    }
}
