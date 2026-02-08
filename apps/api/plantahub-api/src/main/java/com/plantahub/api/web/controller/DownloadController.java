package com.plantahub.api.web.controller;

import com.plantahub.api.repository.DigitalAssetRepository;
import com.plantahub.api.service.DownloadService;
import com.plantahub.api.service.EntitlementService;
import com.plantahub.api.web.dto.downloads.AssetDTO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/me")
public class DownloadController {

    private final EntitlementService entitlementService;
    private final DigitalAssetRepository assetRepo;
    private final DownloadService downloadService;

    public DownloadController(
            EntitlementService entitlementService,
            DigitalAssetRepository assetRepo,
            DownloadService downloadService
    ) {
        this.entitlementService = entitlementService;
        this.assetRepo = assetRepo;
        this.downloadService = downloadService;
    }

    @GetMapping("/downloads/{productId}/{planTypeCode}/assets")
    public List<AssetDTO> listAssets(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable String productId,
            @PathVariable String planTypeCode
    ) {
        String email = user.getUsername();

        // 1) valida direito
        entitlementService.assertHasEntitlement(email, productId, planTypeCode);

        // 2) busca assets no banco
        var assets = assetRepo.findAssetsForProductAndPlanType(productId, planTypeCode.toUpperCase());

        // 3) gera URL pra cada asset
        return assets.stream().map(a -> new AssetDTO(
                a.getFormat().name(),
                a.getVersion(),
                a.getFilename(),
                a.getStorageKey(),
                downloadService.generatePresignedUrl(a.getStorageKey())
        )).toList();
    }
}
