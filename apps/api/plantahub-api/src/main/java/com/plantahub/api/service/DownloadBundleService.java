package com.plantahub.api.service;

import com.plantahub.api.domain.catalog.DigitalAsset;
import com.plantahub.api.domain.downloads.DownloadEntitlement;
import com.plantahub.api.repository.DownloadEntitlementRepository;
import com.plantahub.api.repository.EntitlementAssetRepository;
import com.plantahub.api.shared.storage.ObjectStoragePort;
import com.plantahub.api.web.dto.downloads.CreateDownloadBundleRequest;
import com.plantahub.api.web.dto.downloads.DownloadBundleResponseDTO;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Empacota num unico ZIP os arquivos de varias compras.
 *
 * <p>A estrutura de pastas do ZIP sai dos dados: cada arquivo conhece a propria colecao
 * atraves de {@code product_plan_type}. O layout anterior era identico, mas produzido por
 * um caso especial escrito a mao para uma colecao com nome fixo.
 */
@Service
public class DownloadBundleService {

    private static final long ZIP_URL_EXPIRES_SECONDS = 900L;
    private static final Duration ZIP_URL_DURATION = Duration.ofMinutes(15);

    /**
     * Faz parte da identidade do cache. Foi para "v2" porque o layout do ZIP mudou (passou
     * a preservar subpastas): servir um ZIP montado com o layout antigo entregaria ao
     * cliente uma arvore diferente da que ele veria hoje.
     */
    private static final String BUNDLE_CACHE_VERSION = "v2";

    /**
     * Os ZIPs ficam sob {@code private/} para que uma eventual politica public-read de
     * imagens de produto nunca os alcance.
     */
    private static final String BUNDLE_PREFIX = "private/bundles/";

    private final DownloadEntitlementRepository entitlementRepository;
    private final EntitlementAssetRepository pinnedAssetRepo;
    private final ObjectStoragePort storage;
    private final boolean legacyFallbackEnabled;

    public DownloadBundleService(
            DownloadEntitlementRepository entitlementRepository,
            EntitlementAssetRepository pinnedAssetRepo,
            ObjectStoragePort storage,
            @Value("${app.downloads.legacy-fallback:true}") boolean legacyFallbackEnabled
    ) {
        this.entitlementRepository = entitlementRepository;
        this.pinnedAssetRepo = pinnedAssetRepo;
        this.storage = storage;
        this.legacyFallbackEnabled = legacyFallbackEnabled;
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
        String storageKey = BUNDLE_PREFIX + cacheId + "/" + filename;

        if (storage.exists(storageKey)) {
            return response(filename, storageKey);
        }

        Path tempZip = null;

        try {
            tempZip = Files.createTempFile("plantahub-download-", ".zip");
            writeZip(tempZip, resolvedFiles);
            storage.put(storageKey, tempZip, "application/zip");

            return response(filename, storageKey);
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

    private DownloadBundleResponseDTO response(String filename, String storageKey) {
        return new DownloadBundleResponseDTO(
                filename,
                storageKey,
                storage.presignGet(storageKey, ZIP_URL_DURATION, filename),
                ZIP_URL_EXPIRES_SECONDS
        );
    }

    /**
     * Identidade do conteudo do ZIP.
     *
     * <p>Inclui o caminho dentro do ZIP, e nao apenas a chave de origem: os mesmos arquivos
     * organizados de outra forma sao um ZIP diferente, e servir o antigo entregaria uma
     * arvore que nao corresponde mais ao que o sistema monta.
     */
    private String buildBundleCacheId(List<ResolvedBundleFile> files) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(BUNDLE_CACHE_VERSION.getBytes(StandardCharsets.UTF_8));

            files.stream()
                    .map(f -> f.storageKey() + "|" + f.zipPath())
                    .sorted()
                    .forEach(entry -> digest.update(entry.getBytes(StandardCharsets.UTF_8)));

            return HexFormat.of().formatHex(digest.digest()).substring(0, 16);
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

            for (String code : normalizeCodes(item.planTypeCodes())) {
                if (!seenPairs.add(productId + "::" + code)) {
                    continue;
                }

                DownloadEntitlement entitlement = entitlementRepository
                        .findActiveByUserEmailAndProductIdAndPlanTypeCode(email.toLowerCase(), productId, code)
                        .orElseThrow(() -> new IllegalArgumentException("download_not_entitled"));

                String productFolder = safeFolderName(entitlement.getProduct().getName());

                var pinned = pinnedAssetRepo.findPinnedForDownload(email.toLowerCase(), productId, code);

                if (pinned.isEmpty()) {
                    addLegacyFiles(result, seenStorageKeys, productId, code, productFolder);
                    continue;
                }

                for (var pin : pinned) {
                    DigitalAsset asset = pin.getDigitalAsset();

                    if (seenStorageKeys.add(asset.getStorageKey())) {
                        result.add(new ResolvedBundleFile(asset.getStorageKey(), zipPath(productFolder, asset)));
                    }
                }
            }
        }

        return result;
    }

    /**
     * {@code <Produto>/<colecao do proprio arquivo>/<subpasta>/<arquivo>}.
     *
     * <p>A colecao vem do arquivo, nao da compra: e assim que um arquivo que acompanha toda
     * oferta cai na pasta dele em vez da pasta do que foi comprado — sem nenhum caso
     * especial no codigo.
     */
    private String zipPath(String productFolder, DigitalAsset asset) {
        StringBuilder path = new StringBuilder(productFolder)
                .append('/')
                .append(asset.getProductPlanType().getPlanType().getCode());

        if (asset.getRelativePath() != null && !asset.getRelativePath().isBlank()) {
            path.append('/').append(asset.getRelativePath());
        }

        return path.append('/').append(asset.getFilename()).toString();
    }

    // ------------------------------------------------------------------
    // Compatibilidade com compras anteriores a pinagem. Sai junto com a flag.
    // ------------------------------------------------------------------

    private void addLegacyFiles(List<ResolvedBundleFile> result,
                                Set<String> seenStorageKeys,
                                String productId,
                                String code,
                                String productFolder) {
        if (!legacyFallbackEnabled) {
            throw new IllegalArgumentException("download_assets_not_found");
        }

        List<String> collectionKeys = listKeys("products/" + productId + "/" + code + "/");
        List<String> supportKeys = listKeys("products/" + productId + "/APOIO/");

        if (collectionKeys.isEmpty() && supportKeys.isEmpty()) {
            throw new IllegalArgumentException("download_assets_not_found");
        }

        for (String key : collectionKeys) {
            if (seenStorageKeys.add(key)) {
                result.add(new ResolvedBundleFile(key, productFolder + "/" + code + "/" + extractFilename(key)));
            }
        }

        for (String key : supportKeys) {
            if (seenStorageKeys.add(key)) {
                result.add(new ResolvedBundleFile(key, productFolder + "/APOIO/" + extractFilename(key)));
            }
        }
    }

    private List<String> listKeys(String prefix) {
        return storage.list(prefix).stream().map(o -> o.key()).toList();
    }

    // ------------------------------------------------------------------

    private void writeZip(Path zipPath, List<ResolvedBundleFile> files) throws IOException {
        Set<String> usedPaths = new HashSet<>();

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            for (ResolvedBundleFile file : files) {
                String entryName = uniqueEntryName(usedPaths, sanitizeZipPath(file.zipPath()));

                zos.putNextEntry(new ZipEntry(entryName));

                try (InputStream in = storage.open(file.storageKey())) {
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

    private record ResolvedBundleFile(String storageKey, String zipPath) {}
}
