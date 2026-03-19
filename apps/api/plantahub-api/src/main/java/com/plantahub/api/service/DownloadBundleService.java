package com.plantahub.api.service;

import com.plantahub.api.domain.catalog.DigitalAsset;
import com.plantahub.api.domain.downloads.DownloadEntitlement;
import com.plantahub.api.repository.DigitalAssetRepository;
import com.plantahub.api.repository.DownloadEntitlementRepository;
import com.plantahub.api.web.dto.downloads.CreateDownloadBundleRequest;
import com.plantahub.api.web.dto.downloads.DownloadBundleResponseDTO;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class DownloadBundleService {

    private static final long ZIP_URL_EXPIRES_SECONDS = 900L;
    private static final Duration ZIP_URL_DURATION = Duration.ofMinutes(15);

    private final DownloadEntitlementRepository entitlementRepository;
    private final DigitalAssetRepository digitalAssetRepository;
    private final S3DownloadService s3DownloadService;
    private final S3ObjectStreamService s3ObjectStreamService;

    public DownloadBundleService(
            DownloadEntitlementRepository entitlementRepository,
            DigitalAssetRepository digitalAssetRepository,
            S3DownloadService s3DownloadService,
            S3ObjectStreamService s3ObjectStreamService
    ) {
        this.entitlementRepository = entitlementRepository;
        this.digitalAssetRepository = digitalAssetRepository;
        this.s3DownloadService = s3DownloadService;
        this.s3ObjectStreamService = s3ObjectStreamService;
    }

    @Transactional
    public DownloadBundleResponseDTO createBundle(String email, CreateDownloadBundleRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("download_bundle_items_required");
        }

        List<ResolvedBundleItem> resolvedItems = resolveItems(email, request);

        if (resolvedItems.isEmpty()) {
            throw new IllegalArgumentException("download_bundle_empty");
        }

        String filename = buildZipFilename();
        String storageKey = buildZipStorageKey(email, filename);

        Path tempZip = null;
        try {
            tempZip = Files.createTempFile("plantahub-download-", ".zip");

            writeZip(tempZip, resolvedItems);

            s3DownloadService.uploadFile(storageKey, tempZip, "application/zip");

            String url = s3DownloadService.generatePresignedUrl(storageKey, ZIP_URL_DURATION);

            return new DownloadBundleResponseDTO(
                    filename,
                    storageKey,
                    url,
                    ZIP_URL_EXPIRES_SECONDS
            );
        } catch (IOException e) {
            throw new IllegalStateException("download_bundle_generation_failed", e);
        } finally {
            if (tempZip != null) {
                try {
                    Files.deleteIfExists(tempZip);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private List<ResolvedBundleItem> resolveItems(String email, CreateDownloadBundleRequest request) {
        List<ResolvedBundleItem> result = new ArrayList<>();
        Set<String> seenPairs = new HashSet<>();

        for (CreateDownloadBundleRequest.Item item : request.items()) {
            String productId = normalizeProductId(item.productId());
            Set<String> codes = normalizeCodes(item.planTypeCodes());

            for (String code : codes) {
                String pairKey = productId + "::" + code;
                if (!seenPairs.add(pairKey)) {
                    continue;
                }

                DownloadEntitlement entitlement = entitlementRepository
                        .findActiveByUserEmailAndProductIdAndPlanTypeCode(email.toLowerCase(), productId, code)
                        .orElseThrow(() -> new IllegalArgumentException("download_not_entitled"));

                List<DigitalAsset> assets = digitalAssetRepository
                        .findAllByProductAndPlanType(productId, code);

                if (assets.isEmpty()) {
                    throw new IllegalArgumentException("download_assets_not_found");
                }

                result.add(new ResolvedBundleItem(
                        entitlement.getProduct().getId(),
                        safeFolderName(entitlement.getProduct().getName()),
                        entitlement.getPlanType().getCode(),
                        assets
                ));
            }
        }

        return result;
    }

    private void writeZip(Path zipPath, List<ResolvedBundleItem> items) throws IOException {
        Set<String> usedPaths = new HashSet<>();

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            for (ResolvedBundleItem item : items) {
                for (DigitalAsset asset : item.assets()) {
                    String entryName = uniqueEntryName(
                            usedPaths,
                            item.productFolder() + "/" + item.planTypeCode() + "/" + sanitizeFilename(asset.getFilename())
                    );

                    zos.putNextEntry(new ZipEntry(entryName));

                    try (InputStream in = s3ObjectStreamService.openStream(asset.getStorageKey())) {
                        in.transferTo(zos);
                    }

                    zos.closeEntry();
                }
            }
        }
    }

    private String uniqueEntryName(Set<String> usedPaths, String originalPath) {
        if (usedPaths.add(originalPath)) {
            return originalPath;
        }

        int dot = originalPath.lastIndexOf('.');
        String base = dot >= 0 ? originalPath.substring(0, dot) : originalPath;
        String ext = dot >= 0 ? originalPath.substring(dot) : "";

        int counter = 2;
        while (true) {
            String candidate = base + " (" + counter + ")" + ext;
            if (usedPaths.add(candidate)) {
                return candidate;
            }
            counter++;
        }
    }

    private String buildZipFilename() {
        return "plantahub-download-" + Instant.now().toString().replace(":", "-") + ".zip";
    }

    private String buildZipStorageKey(String email, String filename) {
        return "temp-downloads/" + email.toLowerCase() + "/" + filename;
    }

    private String normalizeProductId(String productId) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("product_id_required");
        }
        return productId.trim();
    }

    private Set<String> normalizeCodes(List<String> planTypeCodes) {
        if (planTypeCodes == null || planTypeCodes.isEmpty()) {
            throw new IllegalArgumentException("plan_type_codes_required");
        }

        LinkedHashSet<String> codes = new LinkedHashSet<>();
        for (String code : planTypeCodes) {
            if (code == null || code.isBlank()) {
                throw new IllegalArgumentException("plan_type_code_invalid");
            }
            codes.add(code.trim().toUpperCase());
        }
        return codes;
    }

    private String safeFolderName(String value) {
        if (value == null || value.isBlank()) {
            return "produto";
        }
        return value.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "arquivo";
        }
        return filename.replace("\\", "_").replace("/", "_");
    }

    private record ResolvedBundleItem(
            String productId,
            String productFolder,
            String planTypeCode,
            List<DigitalAsset> assets
    ) {}
}