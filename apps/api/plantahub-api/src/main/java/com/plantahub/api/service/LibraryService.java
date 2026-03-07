package com.plantahub.api.service;

import com.plantahub.api.domain.catalog.Product;
import com.plantahub.api.repository.DigitalAssetRepository;
import com.plantahub.api.repository.DownloadEntitlementRepository;
import com.plantahub.api.web.dto.Library.LibraryAssetDTO;
import com.plantahub.api.web.dto.Library.LibraryPlanTypeDTO;
import com.plantahub.api.web.dto.Library.LibraryProductDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class LibraryService {

    private final DownloadEntitlementRepository entitlementRepo;
    private final DigitalAssetRepository assetRepo;

    public LibraryService(DownloadEntitlementRepository entitlementRepo,
                          DigitalAssetRepository assetRepo) {
        this.entitlementRepo = entitlementRepo;
        this.assetRepo = assetRepo;
    }

    @Transactional(readOnly = true)
    public List<LibraryProductDTO> myLibrary(String email) {
        var entitlements = entitlementRepo.findActiveByEmail(email);

        if (entitlements.isEmpty()) return List.of();

        // ids para buscar assets em lote
        List<String> productIds = entitlements.stream()
                .map(e -> e.getProduct().getId())
                .distinct()
                .toList();

        List<String> planTypeCodes = entitlements.stream()
                .map(e -> e.getPlanType().getCode())
                .distinct()
                .toList();

        var assets = assetRepo.findAssetsForLibrary(productIds, planTypeCodes);

        // index assets por (productId + planTypeCode)
        Map<String, List<LibraryAssetDTO>> assetsIndex = new HashMap<>();
        for (var a : assets) {
            String pid = a.getProductPlanType().getProduct().getId();
            String code = a.getProductPlanType().getPlanType().getCode();
            String key = pid + "::" + code;

            assetsIndex.computeIfAbsent(key, k -> new ArrayList<>())
                    .add(new LibraryAssetDTO(
                            a.getId().toString(),
                            a.getFilename(),
                            a.getStorageKey(),
                            a.getVersion(),
                            a.getSizeBytes(),
                            a.getCreatedAt()
                    ));
        }

        // montar por produto
        Map<String, ProductBuilder> products = new LinkedHashMap<>();

        for (var e : entitlements) {
            var p = e.getProduct();
            var pt = e.getPlanType();
            var o = e.getOrder();

            var pb = products.computeIfAbsent(p.getId(), id -> new ProductBuilder(p));
            pb.purchasedAt = minInstant(pb.purchasedAt, o.getPaidAt() != null ? o.getPaidAt() : e.getGrantedAt());

            String assetKey = p.getId() + "::" + pt.getCode();
            var assetList = assetsIndex.getOrDefault(assetKey, List.of());

            pb.planTypes.putIfAbsent(pt.getCode(),
                    new LibraryPlanTypeDTO(pt.getCode(), pt.getName(), assetList)
            );
        }

        return products.values().stream()
                .map(ProductBuilder::toDto)
                .toList();
    }

    private Instant minInstant(Instant a, Instant b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isBefore(b) ? a : b;
    }

    private static class ProductBuilder {
        final Product p;
        Instant purchasedAt;
        final Map<String, LibraryPlanTypeDTO> planTypes = new LinkedHashMap<>();

        ProductBuilder(Product p) { this.p = p; }

        LibraryProductDTO toDto() {
            return new LibraryProductDTO(
                    p.getId(),
                    p.getCategory(),
                    p.getSlug(),
                    p.getName(),
                    p.getHeroImageUrl(),
                    p.getAreaM2(),
                    purchasedAt,
                    new ArrayList<>(planTypes.values())
            );
        }
    }
}