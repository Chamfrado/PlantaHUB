package com.plantahub.api.web.controller.admin;

import com.plantahub.api.service.admin.EntitlementBackfillService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Manutencao dos direitos de download ja concedidos. */
@RestController
@RequestMapping("/v1/admin/entitlements")
@PreAuthorize("hasRole('ADMIN')")
public class AdminEntitlementController {

    private final EntitlementBackfillService backfillService;

    public AdminEntitlementController(EntitlementBackfillService backfillService) {
        this.backfillService = backfillService;
    }

    /**
     * Pina os arquivos das compras anteriores a existencia da pinagem.
     *
     * <p>Sincrono de proposito: a operacao e rapida (uma consulta por direito) e o
     * relatorio precisa ser lido na hora — em especial
     * {@code entitlementsWithNoAssets}, que e o sinal de que ainda ha compradores que
     * ficariam sem download se a flag de compatibilidade fosse desligada.
     *
     * <p>{@code dryRun} vem ligado por padrao.
     */
    @PostMapping("/backfill-pins")
    public EntitlementBackfillService.Report backfillPins(
            @RequestParam(defaultValue = "true") boolean dryRun,
            @RequestParam(required = false) String productId
    ) {
        return backfillService.run(dryRun, productId);
    }
}
