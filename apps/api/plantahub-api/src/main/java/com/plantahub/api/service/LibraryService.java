package com.plantahub.api.service;

import com.plantahub.api.domain.catalog.DigitalAsset;
import com.plantahub.api.domain.downloads.DownloadEntitlement;
import com.plantahub.api.repository.DigitalAssetRepository;
import com.plantahub.api.repository.DownloadEntitlementRepository;
import com.plantahub.api.web.dto.library.*;
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
        var entitlements = entitlementRepo.findActiveLibraryByEmail(email.toLowerCase());

        if (entitlements.isEmpty()) {
            return List.of();
        }

        List<String> productIds = entitlements.stream()
                .map(e -> e.getProduct().getId())
                .distinct()
                .toList();

        List<String> planTypeCodes = entitlements.stream()
                .map(e -> e.getPlanType().getCode())
                .distinct()
                .toList();

        var assets = assetRepo.findAssetsForLibrary(productIds, planTypeCodes);

        Map<String, List<LibraryAssetDTO>> assetsIndex = new HashMap<>();
        for (DigitalAsset a : assets) {
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

        Map<String, ProductBuilder> products = new LinkedHashMap<>();

        for (DownloadEntitlement e : entitlements) {
            var p = e.getProduct();
            var pt = e.getPlanType();
            var o = e.getOrder();

            var pb = products.computeIfAbsent(p.getId(), id -> new ProductBuilder(
                    p.getId(),
                    p.getCategory(),
                    p.getSlug(),
                    p.getName(),
                    p.getHeroImageUrl(),
                    p.getAreaM2()
            ));

            Instant referenceDate = o.getPaidAt() != null ? o.getPaidAt() : e.getGrantedAt();
            pb.purchasedAt = minInstant(pb.purchasedAt, referenceDate);

            String assetKey = p.getId() + "::" + pt.getCode();
            var assetList = assetsIndex.getOrDefault(assetKey, List.of());

            pb.planTypes.putIfAbsent(
                    pt.getCode(),
                    new LibraryPlanTypeDTO(
                            pt.getCode(),
                            pt.getName(),
                            assetList
                    )
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
        String productId;
        String category;
        String slug;
        String name;
        String heroImageUrl;
        Integer areaM2;
        Instant purchasedAt;
        Map<String, LibraryPlanTypeDTO> planTypes = new LinkedHashMap<>();

        ProductBuilder(String productId, String category, String slug, String name,
                       String heroImageUrl, Integer areaM2) {
            this.productId = productId;
            this.category = category;
            this.slug = slug;
            this.name = name;
            this.heroImageUrl = heroImageUrl;
            this.areaM2 = areaM2;
        }

        LibraryProductDTO toDto() {
            return new LibraryProductDTO(
                    productId,
                    category,
                    slug,
                    name,
                    heroImageUrl,
                    areaM2,
                    purchasedAt,
                    new ArrayList<>(planTypes.values())
            );
        }
    }
}