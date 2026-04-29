package com.plantahub.api.service;

import com.plantahub.api.domain.catalog.DigitalAsset;
import com.plantahub.api.domain.downloads.DownloadEntitlement;
import com.plantahub.api.repository.DigitalAssetRepository;
import com.plantahub.api.web.dto.downloads.DownloadResponseDTO;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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

        // 2) lista arquivos reais no S3
        List<String> planFiles = s3DownloadService.listKeysByPrefix(basePath);
        List<String> apoioFiles = s3DownloadService.listKeysByPrefix(apoioPath);

        List<String> allFiles = new ArrayList<>();
        allFiles.addAll(planFiles);
        allFiles.addAll(apoioFiles);

        // 3) monta resposta
        var files = allFiles.stream().map(key -> new DownloadResponseDTO.FileDTO(
                extractFilename(key),
                key,
                s3DownloadService.generatePresignedUrl(key, Duration.ofMinutes(15)),
                null // size opcional depois
        )).toList();

        return new DownloadResponseDTO(productId, planTypeCode, files);
    }

    private String extractFilename(String key) {
        int idx = key.lastIndexOf('/');
        return idx >= 0 ? key.substring(idx + 1) : key;
    }


}