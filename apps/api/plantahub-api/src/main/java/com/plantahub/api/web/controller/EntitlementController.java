package com.plantahub.api.web.controller;

import com.plantahub.api.service.EntitlementService;
import com.plantahub.api.web.dto.downloads.DownloadDTO;
import com.plantahub.api.web.dto.orders.MarkPaidResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
public class EntitlementController {

    private final EntitlementService entitlementService;

    public EntitlementController(EntitlementService entitlementService) {
        this.entitlementService = entitlementService;
    }

    // TEMP: dev endpoint (depois vira admin/gateway webhook)
    @PostMapping("/orders/{id}/mark-paid")
    public MarkPaidResponse markPaid(
            @AuthenticationPrincipal Object principal,
            @PathVariable UUID id
    ) {
        String email = extractEmail(principal);
        return entitlementService.markOrderPaid(email, id);
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
