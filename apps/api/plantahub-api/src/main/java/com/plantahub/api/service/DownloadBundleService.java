package com.plantahub.api.service;

import com.plantahub.api.domain.downloads.DownloadEntitlement;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class DownloadBundleService {


    private static final long ZIP_URL_EXPIRES_SECONDS = 900L;
    private static final Duration ZIP_URL_DURATION = Duration.ofMinutes(15);
    private static final String BUNDLE_CACHE_VERSION = "v1";

    private final DownloadEntitlementRepository entitlementRepository;
    private final S3DownloadService s3DownloadService;
    private final S3ObjectStreamService s3ObjectStreamService;

    public DownloadBundleService(
            DownloadEntitlementRepository entitlementRepository,
            S3DownloadService s3DownloadService,
            S3ObjectStreamService s3ObjectStreamService
    ) {
        this.entitlementRepository = entitlementRepository;
        this.s3DownloadService = s3DownloadService;
        this.s3ObjectStreamService = s3ObjectStreamService;
    }

    @Transactional
    public DownloadBundleResponseDTO createBundle(String email, CreateDownloadBundleRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("download_bundle_items_required");
        }

        List<ResolvedBundleFile> resolvedFiles = resolveFiles(email, request);

        if (resolvedFiles.isEmpty()) {
            throw new IllegalArgumentException("download_bundle_empty");
        }

        String cacheId = buildBundleCacheId(resolvedFiles);
        String filename = "plantahub-bundle-" + cacheId + ".zip";
        String storageKey = "bundles/" + cacheId + "/" + filename;

        // If ZIP already exists, just return the URL.
        // This is much faster.
        if (s3DownloadService.objectExists(storageKey)) {
            String url = s3DownloadService.generatePresignedUrl(storageKey, ZIP_URL_DURATION);

            return new DownloadBundleResponseDTO(
                    filename,
                    storageKey,
                    url,
                    ZIP_URL_EXPIRES_SECONDS
            );
        }

        Path tempZip = null;

        try {
            tempZip = Files.createTempFile("plantahub-download-", ".zip");

            writeZip(tempZip, resolvedFiles);

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

    private String buildBundleCacheId(List<ResolvedBundleFile> files) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            List<String> keys = files.stream()
                    .map(ResolvedBundleFile::storageKey)
                    .sorted()
                    .toList();

            digest.update(BUNDLE_CACHE_VERSION.getBytes(StandardCharsets.UTF_8));

            for (String key : keys) {
                digest.update(key.getBytes(StandardCharsets.UTF_8));
            }

            byte[] hash = digest.digest();

            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("bundle_cache_hash_failed", e);
        }
    }

    private List<ResolvedBundleFile> resolveFiles(String email, CreateDownloadBundleRequest request) {
        List<ResolvedBundleFile> result = new ArrayList<>();

        Set<String> seenPairs = new HashSet<>();
        Set<String> seenStorageKeys = new HashSet<>();

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

                String productFolder = safeFolderName(entitlement.getProduct().getName());

                String planPrefix = "products/" + productId + "/" + code + "/";
                String apoioPrefix = "products/" + productId + "/APOIO/";

                List<String> planKeys = s3DownloadService.listKeysByPrefix(planPrefix);
                List<String> apoioKeys = s3DownloadService.listKeysByPrefix(apoioPrefix);

                if (planKeys.isEmpty() && apoioKeys.isEmpty()) {
                    throw new IllegalArgumentException("download_assets_not_found");
                }

                for (String key : planKeys) {
                    if (seenStorageKeys.add(key)) {
                        result.add(new ResolvedBundleFile(
                                key,
                                productFolder + "/" + code + "/" + extractFilename(key)
                        ));
                    }
                }

                for (String key : apoioKeys) {
                    if (seenStorageKeys.add(key)) {
                        result.add(new ResolvedBundleFile(
                                key,
                                productFolder + "/APOIO/" + extractFilename(key)
                        ));
                    }
                }
            }
        }

        return result;
    }

    private void writeZip(Path zipPath, List<ResolvedBundleFile> files) throws IOException {
        Set<String> usedPaths = new HashSet<>();

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            for (ResolvedBundleFile file : files) {
                String entryName = uniqueEntryName(
                        usedPaths,
                        sanitizeZipPath(file.zipPath())
                );

                zos.putNextEntry(new ZipEntry(entryName));

                try (InputStream in = s3ObjectStreamService.openStream(file.storageKey())) {
                    in.transferTo(zos);
                }

                zos.closeEntry();
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

    private String extractFilename(String key) {
        int idx = key.lastIndexOf('/');
        return idx >= 0 ? key.substring(idx + 1) : key;
    }

    private String sanitizeZipPath(String path) {
        if (path == null || path.isBlank()) {
            return "arquivo";
        }

        return path
                .replace("\\", "/")
                .replaceAll("/+", "/")
                .replaceAll("[\\r\\n]", "_");
    }

    private record ResolvedBundleFile(
            String storageKey,
            String zipPath
    ) {}
}