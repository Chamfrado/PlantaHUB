package com.plantahub.api.web.controller.admin;

import com.plantahub.api.service.admin.StorageDiagnosticsService;
import com.plantahub.api.web.dto.admin.StorageDiagnosticsDTOs.DiagnosticsReport;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Diagnóstico do bucket.
 *
 * <p>Só {@code GET}: não existe endpoint para aplicar configuração, de propósito. Ver
 * {@link com.plantahub.api.shared.storage.BucketInspectionPort}.
 */
@RestController
@RequestMapping("/v1/admin/storage")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStorageController {

    private final StorageDiagnosticsService service;

    public AdminStorageController(StorageDiagnosticsService service) {
        this.service = service;
    }

    /**
     * Roda as verificações no momento da chamada.
     *
     * <p>Sem cache: a pergunta que a tela responde é "como está agora", tipicamente logo
     * depois de alguém mexer no console da AWS. Um resultado guardado responderia como
     * estava antes da mudança, que é o pior momento possível para estar desatualizado.
     */
    @GetMapping("/diagnostics")
    public DiagnosticsReport diagnostics() {
        return service.run();
    }
}
