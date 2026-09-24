package com.plantahub.api.web.controller.admin;

import com.plantahub.api.domain.ops.ReconciliationFinding;
import com.plantahub.api.domain.ops.ReconciliationRun;
import com.plantahub.api.repository.AppUserRepository;
import com.plantahub.api.repository.ReconciliationFindingRepository;
import com.plantahub.api.repository.ReconciliationRunRepository;
import com.plantahub.api.service.admin.ReconciliationRunner;
import com.plantahub.api.web.dto.admin.ReconciliationDTOs.FindingDTO;
import com.plantahub.api.web.dto.admin.ReconciliationDTOs.RunDTO;
import com.plantahub.api.web.dto.admin.ReconciliationDTOs.RunStartedDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Reconciliacao banco &harr; bucket.
 *
 * <p>Endpoint administrativo, e nao migration Flyway nem runner de boot, por tres razoes:
 * a operacao precisa de rede e credenciais AWS (migrations tem de ser deterministicas e
 * offline, senao a suite de testes e o CI quebram); precisa ser ensaiada antes de valer
 * (dry run); e precisa ser observada por uma pessoa enquanto roda.
 *
 * <p>{@code @PreAuthorize} aqui e a regra de URL {@code /v1/admin/**} no SecurityConfig
 * sao mantidos os dois: a regra de URL falha fechado para qualquer endpoint que alguem
 * esqueca de anotar, e a anotacao sobrevive a uma mudanca de path.
 */
@RestController
@RequestMapping("/v1/admin/reconciliation")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReconciliationController {

    private final ReconciliationRunRepository runRepo;
    private final ReconciliationFindingRepository findingRepo;
    private final ReconciliationRunner runner;
    private final AppUserRepository userRepo;

    public AdminReconciliationController(ReconciliationRunRepository runRepo,
                                         ReconciliationFindingRepository findingRepo,
                                         ReconciliationRunner runner,
                                         AppUserRepository userRepo) {
        this.runRepo = runRepo;
        this.findingRepo = findingRepo;
        this.runner = runner;
        this.userRepo = userRepo;
    }

    /**
     * Dispara uma varredura e devolve 202 imediatamente.
     *
     * <p>{@code dryRun} vem ligado por padrao: quem quiser escrever no banco precisa pedir
     * explicitamente.
     */
    @PostMapping("/runs")
    public ResponseEntity<RunStartedDTO> start(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(defaultValue = "true") boolean dryRun,
            @RequestParam(required = false) String productId
    ) {
        var startedBy = userRepo.findByEmail(user.getUsername().toLowerCase()).orElse(null);

        // Gravado e commitado ANTES de submeter a tarefa: se a assincrona comecasse antes
        // do commit, ela nao encontraria a propria execucao.
        ReconciliationRun run = runRepo.save(ReconciliationRun.builder()
                .startedBy(startedBy)
                .dryRun(dryRun)
                .productId(productId)
                .status(ReconciliationRun.Status.RUNNING)
                .build());

        runner.runAsync(run.getId());

        return ResponseEntity.accepted()
                .body(new RunStartedDTO(run.getId(), run.getStatus().name()));
    }

    @GetMapping("/runs")
    public Page<RunDTO> list(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "20") int size) {
        return runRepo.findAllByOrderByStartedAtDesc(PageRequest.of(page, Math.min(size, 100)))
                .map(RunDTO::from);
    }

    @GetMapping("/runs/{runId}")
    public ResponseEntity<RunDTO> get(@PathVariable UUID runId) {
        return runRepo.findById(runId)
                .map(RunDTO::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/runs/{runId}/findings")
    public Page<FindingDTO> findings(@PathVariable UUID runId,
                                     @RequestParam(required = false) ReconciliationFinding.Type type,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "50") int size) {
        var pageable = PageRequest.of(page, Math.min(size, 200));

        var result = type == null
                ? findingRepo.findByRun_Id(runId, pageable)
                : findingRepo.findByRun_IdAndType(runId, type, pageable);

        return result.map(FindingDTO::from);
    }
}
