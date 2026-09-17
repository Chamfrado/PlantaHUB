package com.plantahub.api.web.controller;

import com.plantahub.api.service.EntitlementService;
import com.plantahub.api.web.dto.downloads.DownloadDTO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1")
public class EntitlementController {

    private final EntitlementService entitlementService;

    public EntitlementController(EntitlementService entitlementService) {
        this.entitlementService = entitlementService;
    }

    @GetMapping("/me/downloads")
    public List<DownloadDTO> myDownloads(@AuthenticationPrincipal Object principal) {
        String email = extractEmail(principal);
        return entitlementService.myDownloads(email);
    }

    private String extractEmail(Object principal) {
        if (principal == null) return "";
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
            return ud.getUsername();
        }
        return principal.toString();
    }
}
