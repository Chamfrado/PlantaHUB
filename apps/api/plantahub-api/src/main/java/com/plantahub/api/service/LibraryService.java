package com.plantahub.api.service;

import com.plantahub.api.domain.catalog.DigitalAsset;
import com.plantahub.api.domain.downloads.DownloadEntitlement;
import com.plantahub.api.domain.downloads.EntitlementAsset;
import com.plantahub.api.repository.DownloadEntitlementRepository;
import com.plantahub.api.repository.EntitlementAssetRepository;
import com.plantahub.api.shared.storage.ObjectStoragePort;
import com.plantahub.api.shared.storage.StoredObject;
import com.plantahub.api.web.dto.library.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/**
 * A biblioteca do cliente: o que ele comprou e quais arquivos vieram junto.
 *
 * <p>Passou a ser resolvida em duas consultas SQL. Antes, montar esta tela custava uma
 * listagem completa no S3 <b>por produto, a cada requisicao</b>, e os identificadores dos
 * arquivos eram um hash do caminho em vez do id real da linha.
 */
@Service
public class LibraryService {

    private final DownloadEntitlementRepository entitlementRepo;
    private final EntitlementAssetRepository pinnedAssetRepo;
    private final ObjectStoragePort storage;
    private final boolean legacyFallbackEnabled;

    public LibraryService(
            DownloadEntitlementRepository entitlementRepo,
            EntitlementAssetRepository pinnedAssetRepo,
            ObjectStoragePort storage,
            @Value("${app.downloads.legacy-fallback:true}") boolean legacyFallbackEnabled
    ) {
        this.entitlementRepo = entitlementRepo;
        this.pinnedAssetRepo = pinnedAssetRepo;
        this.storage = storage;
        this.legacyFallbackEnabled = legacyFallbackEnabled;
    }

    @Transactional(readOnly = true)
    public List<LibraryProductDTO> myLibrary(String email) {
        String normalizedEmail = email.toLowerCase();

        var entitlements = entitlementRepo.findActiveLibraryByEmail(normalizedEmail);

        if (entitlements.isEmpty()) {
            return List.of();
        }

        // Todos os arquivos de todas as compras, numa consulta so.
        Map<UUID, List<DigitalAsset>> assetsByEntitlement = new HashMap<>();
        for (EntitlementAsset pin : pinnedAssetRepo.findPinnedForLibrary(normalizedEmail)) {
            assetsByEntitlement
                    .computeIfAbsent(pin.getEntitlement().getId(), id -> new ArrayList<>())
                    .add(pin.getDigitalAsset());
        }

        Map<String, ProductBuilder> products = new LinkedHashMap<>();

        // Carregado sob demanda: so uma compra sem arquivos pinados precisa do S3, e
        // enquanto nao houver nenhuma, esta tela nao toca no bucket.
        Map<String, ProductS3Index> legacyIndexByProduct = new HashMap<>();

        for (DownloadEntitlement e : entitlements) {
            var p = e.getProduct();
            var pt = e.getPlanType();
            var o = e.getOrder();

            var pb = products.computeIfAbsent(p.getId(), id -> new ProductBuilder(
                    p.getId(), p.getCategory(), p.getSlug(), p.getName(),
                    p.getHeroImageUrl(), p.getAreaM2()
            ));

            Instant referenceDate = o.getPaidAt() != null ? o.getPaidAt() : e.getGrantedAt();
            pb.purchasedAt = minInstant(pb.purchasedAt, referenceDate);

            String planTypeCode = pt.getCode().toUpperCase();

            List<DigitalAsset> pinned = assetsByEntitlement.get(e.getId());

            List<LibraryAssetDTO> assets = pinned != null && !pinned.isEmpty()
                    ? pinned.stream().map(this::toLibraryAsset).toList()
                    : legacyAssets(legacyIndexByProduct, p.getId(), planTypeCode);

            pb.planTypes.putIfAbsent(
                    planTypeCode,
                    new LibraryPlanTypeDTO(planTypeCode, pt.getName(), assets)
            );
        }

        return products.values().stream().map(ProductBuilder::toDto).toList();
    }

    private LibraryAssetDTO toLibraryAsset(DigitalAsset asset) {
        return new LibraryAssetDTO(
                asset.getId().toString(),
                asset.getFilename(),
                asset.getStorageKey(),
                asset.getVersion(),
                asset.getSizeBytes(),
                asset.getCreatedAt()
        );
    }

    // ------------------------------------------------------------------
    // Compatibilidade com compras anteriores a pinagem.
    //
    // Tudo abaixo existe apenas para que uma compra sem arquivos pinados continue
    // aparecendo na biblioteca. E o unico trecho deste servico que ainda monta chave por
    // convencao e que ainda conhece o nome de uma colecao especifica. Sai junto com a
    // flag, assim que o backfill reportar que nao ha mais compras nessa situacao.
    // ------------------------------------------------------------------

    private List<LibraryAssetDTO> legacyAssets(Map<String, ProductS3Index> cache,
                                               String productId,
                                               String planTypeCode) {
        if (!legacyFallbackEnabled) {
            return List.of();
        }

        ProductS3Index index = cache.computeIfAbsent(productId, this::loadLegacyProductIndex);

        LinkedHashMap<String, StoredObject> files = new LinkedHashMap<>();

        index.byFolder().getOrDefault(planTypeCode, List.of())
                .forEach(file -> files.put(file.key(), file));
        index.byFolder().getOrDefault("APOIO", List.of())
                .forEach(file -> files.put(file.key(), file));

        return files.values().stream().map(this::toLegacyLibraryAsset).toList();
    }

    private ProductS3Index loadLegacyProductIndex(String productId) {
        String productPrefix = "products/" + productId + "/";

        Map<String, List<StoredObject>> byFolder = new HashMap<>();

        for (StoredObject file : storage.list(productPrefix)) {
            String folder = extractLegacyFolder(productId, file.key());

            if (folder == null || folder.isBlank()) {
                continue;
            }

            byFolder.computeIfAbsent(folder.toUpperCase(), key -> new ArrayList<>()).add(file);
        }

        return new ProductS3Index(byFolder);
    }

    private String extractLegacyFolder(String productId, String key) {
        String prefix = "products/" + productId + "/";

        if (!key.startsWith(prefix)) {
            return null;
        }

        String remaining = key.substring(prefix.length());
        int slash = remaining.indexOf('/');

        return slash < 0 ? null : remaining.substring(0, slash);
    }

    private LibraryAssetDTO toLegacyLibraryAsset(StoredObject file) {
        return new LibraryAssetDTO(
                // Sem linha no banco, o identificador e derivado da chave.
                UUID.nameUUIDFromBytes(file.key().getBytes(StandardCharsets.UTF_8)).toString(),
                extractFilename(file.key()),
                file.key(),
                1,
                file.sizeBytes(),
                file.lastModified()
        );
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
                    productId, category, slug, name, heroImageUrl, areaM2,
                    purchasedAt, new ArrayList<>(planTypes.values())
            );
        }
    }

    private record ProductS3Index(Map<String, List<StoredObject>> byFolder) {}
}
