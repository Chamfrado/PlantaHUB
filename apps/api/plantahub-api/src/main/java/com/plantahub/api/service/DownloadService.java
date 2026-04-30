package com.plantahub.api.service;

import com.plantahub.api.repository.DigitalAssetRepository;
import com.plantahub.api.web.dto.downloads.DownloadResponseDTO;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Service
public class DownloadService {

    private final EntitlementService entitlementService;
    private final DigitalAssetRepository assetRepo;
    private final S3DownloadService s3DownloadService;

    public DownloadService(
            EntitlementService entitlementService,
            DigitalAssetRepository assetRepo,
            S3DownloadService s3DownloadService
    ) {
        this.entitlementService = entitlementService;
        this.assetRepo = assetRepo;
        this.s3DownloadService = s3DownloadService;
    }


    public DownloadResponseDTO downloadAll(String email, String productId, String planTypeCode) {

        // 1) valida entitlement
        entitlementService.validateEntitlement(email, productId, planTypeCode);



        String basePath = "products/" + productId + "/" + planTypeCode + "/";
        String apoioPath = "products/" + productId + "/APOIO/";

        // 2) lista arquivos reais no S3 com metadados
        List<S3DownloadService.S3AssetInfo> planFiles =
                s3DownloadService.listAssetsByPrefix(basePath);

        List<S3DownloadService.S3AssetInfo> apoioFiles =
                s3DownloadService.listAssetsByPrefix(apoioPath);

        Map<String, S3DownloadService.S3AssetInfo> allFiles = new LinkedHashMap<>();

        for (S3DownloadService.S3AssetInfo file : planFiles) {
            allFiles.put(file.key(), file);
        }

        for (S3DownloadService.S3AssetInfo file : apoioFiles) {
            allFiles.put(file.key(), file);
        }



        // 3) monta resposta com tamanho real
        var files = allFiles.values().stream().map(file -> new DownloadResponseDTO.FileDTO(
                extractFilename(file.key()),
                file.key(),
                s3DownloadService.generatePresignedUrl(file.key(), Duration.ofMinutes(15)),
                file.sizeBytes()
        )).toList();

        return new DownloadResponseDTO(productId, planTypeCode, files);
    }

    private String extractFilename(String key) {
        int idx = key.lastIndexOf('/');
        return idx >= 0 ? key.substring(idx + 1) : key;
    }


}