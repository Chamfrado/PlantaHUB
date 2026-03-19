package com.plantahub.api.web.controller;

import com.plantahub.api.service.DownloadBundleService;
import com.plantahub.api.service.DownloadService;
import com.plantahub.api.web.dto.downloads.CreateDownloadBundleRequest;
import com.plantahub.api.web.dto.downloads.DownloadBundleResponseDTO;
import com.plantahub.api.web.dto.downloads.DownloadResponseDTO;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/me/downloads")
public class DownloadController {

    private final DownloadService downloadService;
    private final DownloadBundleService downloadBundleService;

    public DownloadController(
            DownloadService downloadService,
            DownloadBundleService downloadBundleService
    ) {
        this.downloadService = downloadService;
        this.downloadBundleService = downloadBundleService;
    }

    @GetMapping("/products/{productId}/plan-types/{planTypeCode}")
    public DownloadResponseDTO downloadSpecificPlan(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable String productId,
            @PathVariable String planTypeCode
    ) {
        return downloadService.downloadAll(
                user.getUsername(),
                productId,
                planTypeCode.toUpperCase()
        );
    }

    @PostMapping("/bundle")
    public DownloadBundleResponseDTO createBundle(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody CreateDownloadBundleRequest request
    ) {
        return downloadBundleService.createBundle(user.getUsername(), request);
    }
}