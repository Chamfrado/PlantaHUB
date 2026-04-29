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
        Map<String, ProductS3Index> s3IndexByProduct = new HashMap<>();
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

            ProductS3Index s3Index = s3IndexByProduct.computeIfAbsent(
                    p.getId(),
                    this::loadProductS3Index
            );

            List<LibraryAssetDTO> assets = resolveAssetsFromIndex(s3Index, planTypeCode);

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

        List<S3DownloadService.S3AssetInfo> planFiles =
                s3DownloadService.listAssetsByPrefix(planPrefix);

        List<S3DownloadService.S3AssetInfo> apoioFiles =
                s3DownloadService.listAssetsByPrefix(apoioPrefix);

        Map<String, S3DownloadService.S3AssetInfo> allFiles = new LinkedHashMap<>();

        for (S3DownloadService.S3AssetInfo file : planFiles) {
            allFiles.put(file.key(), file);
        }

        for (S3DownloadService.S3AssetInfo file : apoioFiles) {
            allFiles.put(file.key(), file);
        }

        return allFiles.values().stream()
                .map(this::toLibraryAsset)
                .toList();
    }

    private ProductS3Index loadProductS3Index(String productId) {
        String productPrefix = "products/" + productId + "/";

        List<S3DownloadService.S3AssetInfo> allFiles =
                s3DownloadService.listAssetsByPrefix(productPrefix);

        Map<String, List<S3DownloadService.S3AssetInfo>> byFolder = new HashMap<>();

        for (S3DownloadService.S3AssetInfo file : allFiles) {
            String folder = extractFolderAfterProduct(productId, file.key());

            if (folder == null || folder.isBlank()) {
                continue;
            }

            byFolder.computeIfAbsent(folder.toUpperCase(), key -> new ArrayList<>())
                    .add(file);
        }

        return new ProductS3Index(byFolder);
    }

    private String extractFolderAfterProduct(String productId, String key) {
        String prefix = "products/" + productId + "/";

        if (!key.startsWith(prefix)) {
            return null;
        }

        String remaining = key.substring(prefix.length());

        int slash = remaining.indexOf('/');

        if (slash < 0) {
            return null;
        }

        return remaining.substring(0, slash);
    }

    private List<LibraryAssetDTO> resolveAssetsFromIndex(ProductS3Index index, String planTypeCode) {
        LinkedHashMap<String, S3DownloadService.S3AssetInfo> files = new LinkedHashMap<>();

        index.byFolder()
                .getOrDefault(planTypeCode.toUpperCase(), List.of())
                .forEach(file -> files.put(file.key(), file));

        index.byFolder()
                .getOrDefault("APOIO", List.of())
                .forEach(file -> files.put(file.key(), file));

        return files.values().stream()
                .map(this::toLibraryAsset)
                .toList();
    }

    private LibraryAssetDTO toLibraryAsset(S3DownloadService.S3AssetInfo file) {
        return new LibraryAssetDTO(
                stableId(file.key()),
                extractFilename(file.key()),
                file.key(),
                1,
                file.sizeBytes(),
                file.lastModified()
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

    private record ProductS3Index(
            Map<String, List<S3DownloadService.S3AssetInfo>> byFolder
    ) {}

}