package com.plantahub.api.service.admin;

import com.plantahub.api.domain.catalog.PlanType;
import com.plantahub.api.domain.catalog.Product;
import com.plantahub.api.domain.catalog.ProductPlanType;
import com.plantahub.api.repository.DigitalAssetRepository;
import com.plantahub.api.repository.OrderRepository;
import com.plantahub.api.repository.PlanTypeRepository;
import com.plantahub.api.repository.ProductPlanTypeRepository;
import com.plantahub.api.repository.ProductRepository;
import com.plantahub.api.shared.exception.ConflictException;
import com.plantahub.api.shared.exception.NotFoundException;
import com.plantahub.api.web.dto.admin.AdminOfferDTOs.OfferDTO;
import com.plantahub.api.web.dto.admin.AdminOfferDTOs.OfferRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * O que um produto vende, e por quanto.
 *
 * <p>Uma "oferta" e a linha que liga produto a colecao com preco. E tambem o unico
 * ancoradouro de um arquivo, e por isso existem ofertas nao vendaveis: uma colecao de
 * anexos precisa da linha para ter onde pendurar arquivos, sem nunca virar opcao de compra.
 */
@Service
public class AdminOfferService {

    private final ProductRepository productRepo;
    private final PlanTypeRepository collectionRepo;
    private final ProductPlanTypeRepository offerRepo;
    private final DigitalAssetRepository assetRepo;
    private final OrderRepository orderRepo;

    public AdminOfferService(ProductRepository productRepo,
                             PlanTypeRepository collectionRepo,
                             ProductPlanTypeRepository offerRepo,
                             DigitalAssetRepository assetRepo,
                             OrderRepository orderRepo) {
        this.productRepo = productRepo;
        this.collectionRepo = collectionRepo;
        this.offerRepo = offerRepo;
        this.assetRepo = assetRepo;
        this.orderRepo = orderRepo;
    }

    /**
     * Devolve DTOs, e nao entidades.
     *
     * <p>Mapear no controller deixaria a leitura da colecao acontecer depois que a
     * transacao fechou — e com {@code open-in-view: false} isso e um 500 em producao.
     * Montar aqui dentro torna esse erro impossivel por construcao.
     */
    @Transactional(readOnly = true)
    public List<OfferDTO> list(String productId) {
        requireProduct(productId);
        return offerRepo.findAllByProductIdWithPlanType(productId).stream()
                .map(OfferDTO::from)
                .toList();
    }

    /** Cria a oferta se nao existir; caso contrario atualiza a existente. */
    @Transactional
    public OfferDTO upsert(String productId, String collectionCode, OfferRequest request) {
        Product product = requireProduct(productId);
        String code = collectionCode.trim().toUpperCase(Locale.ROOT);

        PlanType collection = collectionRepo.findByCode(code)
                .orElseThrow(() -> new NotFoundException("collection_not_found: " + code));

        ProductPlanType offer = offerRepo.findByProduct_IdAndPlanType_Code(productId, code)
                .orElseGet(() -> ProductPlanType.builder()
                        .product(product)
                        .planType(collection)
                        .priceCents(0)
                        .includedInBundle(false)
                        .available(false)
                        .sortOrder(0)
                        .build());

        if (request.priceCents() != null) offer.setPriceCents(request.priceCents());
        if (request.available() != null) offer.setAvailable(request.available());
        if (request.includedInBundle() != null) offer.setIncludedInBundle(request.includedInBundle());
        if (request.sortOrder() != null) offer.setSortOrder(request.sortOrder());

        return OfferDTO.from(offerRepo.save(offer));
    }

    /**
     * Remove a oferta de um produto.
     *
     * <p>Bloqueada quando ha arquivos ancorados ou vendas: nos dois casos a remocao
     * destruiria dados. O caminho para "parar de vender" e {@code available = false}.
     */
    @Transactional
    public void remove(String productId, String collectionCode) {
        String code = collectionCode.trim().toUpperCase(Locale.ROOT);

        ProductPlanType offer = offerRepo.findByProduct_IdAndPlanType_Code(productId, code)
                .orElseThrow(() -> new NotFoundException("offer_not_found"));

        if (assetRepo.countActiveByProductPlanTypeId(offer.getId()) > 0) {
            throw new ConflictException("offer_has_assets_set_unavailable_instead");
        }

        if (orderRepo.existsBySelectionProductAndPlanType(productId, code)) {
            throw new ConflictException("offer_has_sales_set_unavailable_instead");
        }

        offerRepo.delete(offer);
    }

    @Transactional
    public void reorder(String productId, List<String> collectionCodes) {
        requireProduct(productId);

        int order = 0;

        for (String rawCode : collectionCodes) {
            String code = rawCode.trim().toUpperCase(Locale.ROOT);

            ProductPlanType offer = offerRepo.findByProduct_IdAndPlanType_Code(productId, code)
                    .orElseThrow(() -> new NotFoundException("offer_not_found: " + code));

            offer.setSortOrder(order++);
            offerRepo.save(offer);
        }
    }

    private Product requireProduct(String productId) {
        return productRepo.findById(productId)
                .orElseThrow(() -> new NotFoundException("product_not_found"));
    }
}
