package com.plantahub.api.service;

import com.plantahub.api.domain.downloads.DownloadEntitlement;
import com.plantahub.api.repository.DownloadEntitlementRepository;
import com.plantahub.api.web.dto.library.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service
public class LibraryService {

    private final DownloadEntitlementRepository entitlementRepo;
    private final S3DownloadService s3DownloadService;

    public LibraryService(
            DownloadEntitlementRepository entitlementRepo,
            S3DownloadService s3DownloadService
    ) {
        this.entitlementRepo = entitlementRepo;
        this.s3DownloadService = s3DownloadService;
    }

    @Transactional(readOnly = true)
    public List<LibraryProductDTO> myLibrary(String email) {
        var entitlements = entitlementRepo.findActiveLibraryByEmail(email.toLowerCase());

        if (entitlements.isEmpty()) {
            return List.of();
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

            String planTypeCode = pt.getCode().toUpperCase();

            List<LibraryAssetDTO> assets = resolveAssetsFromS3(p.getId(), planTypeCode);

            pb.planTypes.putIfAbsent(
                    planTypeCode,
                    new LibraryPlanTypeDTO(
                            planTypeCode,
                            pt.getName(),
                            assets
                    )
            );
        }

        return products.values().stream()
                .map(ProductBuilder::toDto)
                .toList();
    }

    private List<LibraryAssetDTO> resolveAssetsFromS3(String productId, String planTypeCode) {
        String planPrefix = "products/" + productId + "/" + planTypeCode + "/";
        String apoioPrefix = "products/" + productId + "/APOIO/";

        List<String> planKeys = s3DownloadService.listKeysByPrefix(planPrefix);
        List<String> apoioKeys = s3DownloadService.listKeysByPrefix(apoioPrefix);

        List<String> allKeys = new ArrayList<>();
        allKeys.addAll(planKeys);
        allKeys.addAll(apoioKeys);

        return allKeys.stream()
                .distinct()
                .map(this::toLibraryAsset)
                .toList();
    }

    private LibraryAssetDTO toLibraryAsset(String storageKey) {
        return new LibraryAssetDTO(
                stableId(storageKey),
                extractFilename(storageKey),
                storageKey,
                1,
                null,
                null
        );
    }

    private String stableId(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private String extractFilename(String key) {
        int idx = key.lastIndexOf('/');
        return idx >= 0 ? key.substring(idx + 1) : key;
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