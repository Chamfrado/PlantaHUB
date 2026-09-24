package com.plantahub.api.service.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Dispara uma varredura fora da requisicao HTTP.
 *
 * <p>Bean separado de {@link ReconciliationService} de proposito: {@code @Async} e
 * {@code @Transactional} funcionam por proxy, entao um metodo chamando o outro dentro da
 * mesma classe passaria direto pelo proxy e rodaria sincrono, na transacao do chamador —
 * silenciosamente, sem erro nenhum.
 */
@Component
public class ReconciliationRunner {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationRunner.class);

    private final ReconciliationService service;

    public ReconciliationRunner(ReconciliationService service) {
        this.service = service;
    }

    @Async("adminTaskExecutor")
    public void runAsync(UUID runId) {
        try {
            service.execute(runId);
        } catch (Exception e) {
            log.error("Reconciliacao {} falhou", runId, e);
            // Numa transacao propria: a de execute() ja sofreu rollback, e sem isto a
            // varredura ficaria eternamente marcada como RUNNING.
            service.markFailed(runId, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
