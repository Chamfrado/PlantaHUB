package com.plantahub.api.service;

import com.plantahub.api.domain.catalog.DigitalAsset;
import com.plantahub.api.repository.EntitlementAssetRepository;
import com.plantahub.api.shared.storage.ObjectStoragePort;
import com.plantahub.api.shared.storage.StoredObject;
import com.plantahub.api.web.dto.downloads.DownloadResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Entrega os arquivos de uma colecao comprada.
 *
 * <p>A lista de arquivos vem de {@code entitlement_asset}, congelada no momento do
 * pagamento. Antes ela era descoberta listando prefixos no S3, o que significava que o
 * sistema montava chaves por convencao — a origem do {@code NoSuchKey} quando a convencao
 * assumida pelo codigo divergia do que estava no bucket.
 */
@Service
public class DownloadService {

    private static final Duration URL_DURATION = Duration.ofMinutes(15);

    private final EntitlementService entitlementService;
    private final EntitlementAssetRepository pinnedAssetRepo;
    private final ObjectStoragePort storage;
    private final boolean legacyFallbackEnabled;

    public DownloadService(
            EntitlementService entitlementService,
            EntitlementAssetRepository pinnedAssetRepo,
            ObjectStoragePort storage,
            @Value("${app.downloads.legacy-fallback:true}") boolean legacyFallbackEnabled
    ) {
        this.entitlementService = entitlementService;
        this.pinnedAssetRepo = pinnedAssetRepo;
        this.storage = storage;
        this.legacyFallbackEnabled = legacyFallbackEnabled;
    }

    @Transactional(readOnly = true)
    public DownloadResponseDTO downloadAll(String email, String productId, String planTypeCode) {
        // Continua sendo o portao: exige direito vivo num pedido pago.
        entitlementService.validateEntitlement(email, productId, planTypeCode);

        var pinned = pinnedAssetRepo.findPinnedForDownload(email, productId, planTypeCode);

        List<DownloadResponseDTO.FileDTO> files = pinned.isEmpty()
                ? legacyFiles(productId, planTypeCode)
                : pinned.stream().map(ea -> toFile(ea.getDigitalAsset())).toList();

        return new DownloadResponseDTO(productId, planTypeCode, files);
    }

    private DownloadResponseDTO.FileDTO toFile(DigitalAsset asset) {
        return new DownloadResponseDTO.FileDTO(
                asset.getFilename(),
                asset.getStorageKey(),
                storage.presignGet(asset.getStorageKey(), URL_DURATION, asset.getFilename()),
                asset.getSizeBytes()
        );
    }

    /**
     * Comportamento anterior a pinagem, preservado apenas aqui dentro.
     *
     * <p>Uma compra feita antes desta modernizacao pode ainda nao ter arquivos pinados. Sem
     * esta saida, ela deixaria de baixar de um dia para o outro. O relatorio de backfill
     * (campo {@code entitlementsWithNoAssets}) diz quando nao existe mais nenhuma compra
     * nessa situacao; a partir dai a flag pode ser desligada e este metodo, removido.
     *
     * <p>E o unico lugar do sistema que ainda monta chave por convencao e que ainda conhece
     * o nome de uma colecao especifica.
     */
    private List<DownloadResponseDTO.FileDTO> legacyFiles(String productId, String planTypeCode) {
        if (!legacyFallbackEnabled) {
            return List.of();
        }

        String collectionPrefix = "products/" + productId + "/" + planTypeCode + "/";
        String supportPrefix = "products/" + productId + "/APOIO/";

        Map<String, StoredObject> merged = new LinkedHashMap<>();
        storage.list(collectionPrefix).forEach(file -> merged.put(file.key(), file));
        storage.list(supportPrefix).forEach(file -> merged.put(file.key(), file));

        return merged.values().stream()
                .map(file -> new DownloadResponseDTO.FileDTO(
                        extractFilename(file.key()),
                        file.key(),
                        storage.presignGet(file.key(), URL_DURATION),
                        file.sizeBytes()
                ))
                .toList();
    }

    private String extractFilename(String key) {
        int idx = key.lastIndexOf('/');
        return idx >= 0 ? key.substring(idx + 1) : key;
    }
}
