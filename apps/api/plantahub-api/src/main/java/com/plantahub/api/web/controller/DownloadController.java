package com.plantahub.api.web.controller;

import com.plantahub.api.service.DownloadService;
import com.plantahub.api.web.dto.downloads.DownloadResponseDTO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1")
public class DownloadController {

    private final DownloadService downloadService;

    public DownloadController(DownloadService downloadService) {
        this.downloadService = downloadService;
    }

    @GetMapping("/me/downloads/{productId}/{planTypeCode}")
    public DownloadResponseDTO download(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable String productId,
            @PathVariable String planTypeCode
    ) {
        return downloadService.downloadAll(user.getUsername(), productId, planTypeCode.toUpperCase());
    }
}