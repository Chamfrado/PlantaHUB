package com.plantahub.api.service.admin;

import com.plantahub.api.domain.catalog.PlanType;
import com.plantahub.api.repository.DigitalAssetRepository;
import com.plantahub.api.repository.PlanTypeRepository;
import com.plantahub.api.repository.ProductPlanTypeRepository;
import com.plantahub.api.shared.exception.ConflictException;
import com.plantahub.api.shared.exception.NotFoundException;
import com.plantahub.api.web.dto.admin.AdminCollectionDTOs.CreateCollectionRequest;
import com.plantahub.api.web.dto.admin.AdminCollectionDTOs.UpdateCollectionRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * CRUD das colecoes de arquivos.
 *
 * <p>Depois desta fase, {@code ARCH}, {@code APOIO} e companhia sao linhas criadas pelo
 * admin. Nada no sistema conhece nenhum codigo de antemao.
 */
@Service
public class AdminCollectionService {

    private static final Pattern VALID_CODE = Pattern.compile("^[A-Z][A-Z0-9_]{1,39}$");

    private final PlanTypeRepository collectionRepo;
    private final ProductPlanTypeRepository offerRepo;
    private final DigitalAssetRepository assetRepo;

    public AdminCollectionService(PlanTypeRepository collectionRepo,
                                  ProductPlanTypeRepository offerRepo,
                                  DigitalAssetRepository assetRepo) {
        this.collectionRepo = collectionRepo;
        this.offerRepo = offerRepo;
        this.assetRepo = assetRepo;
    }

    @Transactional(readOnly = true)
    public List<PlanType> list() {
        return collectionRepo.findAll().stream()
                .sorted((a, b) -> {
                    int bySort = Integer.compare(
                            a.getSortOrder() == null ? 0 : a.getSortOrder(),
                            b.getSortOrder() == null ? 0 : b.getSortOrder());
                    return bySort != 0 ? bySort : a.getCode().compareTo(b.getCode());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public PlanType get(UUID id) {
        return collectionRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("collection_not_found"));
    }

    @Transactional
    public PlanType create(CreateCollectionRequest request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);

        if (!VALID_CODE.matcher(code).matches()) {
            throw new ConflictException("collection_code_invalid: " + code);
        }

        collectionRepo.findByCode(code).ifPresent(existing -> {
            throw new ConflictException("collection_code_taken: " + code);
        });

        return collectionRepo.save(PlanType.builder()
                .code(code)
                .name(request.name())
                .description(request.description())
                .purchasable(request.purchasable() == null || request.purchasable())
                .bundledWithEveryOffer(request.bundledWithEveryOffer() != null && request.bundledWithEveryOffer())
                .active(true)
                .sortOrder(request.sortOrder() == null ? 0 : request.sortOrder())
                .build());
    }

    /**
     * Atualiza a colecao. O {@code code} <b>nao</b> pode mudar.
     *
     * <p>Ele esta gravado dentro das chaves S3 legadas e e o que o mapeamento
     * pasta&rarr;colecao usa na reconciliacao e no upload de pastas. Renomea-lo orfanaria,
     * em silencio, todo arquivo legado daquela pasta.
     */
    @Transactional
    public PlanType update(UUID id, UpdateCollectionRequest request) {
        PlanType collection = get(id);

        if (request.code() != null
                && !request.code().trim().equalsIgnoreCase(collection.getCode())) {
            throw new ConflictException("collection_code_immutable");
        }

        if (request.name() != null) collection.setName(request.name());
        if (request.description() != null) collection.setDescription(request.description());
        if (request.purchasable() != null) collection.setPurchasable(request.purchasable());
        if (request.bundledWithEveryOffer() != null) {
            collection.setBundledWithEveryOffer(request.bundledWithEveryOffer());
        }
        if (request.sortOrder() != null) collection.setSortOrder(request.sortOrder());

        return collectionRepo.save(collection);
    }

    @Transactional
    public PlanType setActive(UUID id, boolean active) {
        PlanType collection = get(id);
        collection.setActive(active);
        return collectionRepo.save(collection);
    }

    @Transactional
    public void delete(UUID id) {
        PlanType collection = get(id);

        if (!offerRepo.findAllByPlanType_Id(id).isEmpty()) {
            // Apagar levaria junto os arquivos ancorados nela e os direitos ja concedidos.
            throw new ConflictException("collection_in_use_deactivate_instead");
        }

        collectionRepo.delete(collection);
    }

    /** Quantos arquivos vivos existem nesta colecao, somando todos os produtos. */
    @Transactional(readOnly = true)
    public long assetCount(UUID collectionId) {
        return assetRepo.countActiveByCollectionId(collectionId);
    }
}
