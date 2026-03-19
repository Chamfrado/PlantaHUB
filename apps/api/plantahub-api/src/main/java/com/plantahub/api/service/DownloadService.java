package com.plantahub.api.service;

import com.plantahub.api.domain.catalog.DigitalAsset;
import com.plantahub.api.domain.downloads.DownloadEntitlement;
import com.plantahub.api.repository.DigitalAssetRepository;
import com.plantahub.api.web.dto.downloads.DownloadResponseDTO;
import org.springframework.stereotype.Service;

import java.time.Duration;
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
        DownloadEntitlement ent = entitlementService.validateEntitlement(email, productId, planTypeCode);

        // 2) lista assets do planType
        List<DigitalAsset> assets = assetRepo.findAllByProductAndPlanType(productId, planTypeCode);

        // 3) gera URL para cada asset
        var files = assets.stream().map(a -> new DownloadResponseDTO.FileDTO(
                a.getFilename(),
                a.getStorageKey(),
                s3DownloadService.generatePresignedUrl(a.getStorageKey(), Duration.ofMinutes(15)),
                a.getSizeBytes()
        )).toList();

        return new DownloadResponseDTO(productId, planTypeCode, files);
    }


}