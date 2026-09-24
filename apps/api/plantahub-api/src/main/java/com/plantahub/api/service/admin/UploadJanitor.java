package com.plantahub.api.service.admin;

import com.plantahub.api.domain.uploads.PendingUpload;
import com.plantahub.api.repository.PendingUploadRepository;
import com.plantahub.api.shared.storage.ObjectStoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Fecha uploads que começaram e nunca terminaram.
 *
 * <p>A conexão cai, a aba fecha, o navegador é encerrado. O registro fica pendente e, se
 * era multipart, as partes já enviadas <b>continuam sendo cobradas</b> sem aparecer na
 * listagem normal do bucket.
 *
 * <p><b>Nunca apaga objeto.</b> Abortar um multipart descarta partes incompletas, que não
 * são arquivo de ninguém. Um objeto já gravado por um PUT que nunca foi confirmado
 * permanece: apagá-lo automaticamente correria o risco de remover algo que uma confirmação
 * atrasada ainda vai registrar. Ele é recolhido pela regra de ciclo de vida do bucket.
 */
@Component
public class UploadJanitor {

    private static final Logger log = LoggerFactory.getLogger(UploadJanitor.class);

    private final PendingUploadRepository pendingRepo;
    private final ObjectStoragePort storage;
    private final boolean enabled;

    public UploadJanitor(PendingUploadRepository pendingRepo,
                         ObjectStoragePort storage,
                         @Value("${app.uploads.janitor.enabled:true}") boolean enabled) {
        this.pendingRepo = pendingRepo;
        this.storage = storage;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${app.uploads.janitor.interval-ms:3600000}")
    @Transactional
    public void sweep() {
        if (!enabled) {
            return;
        }

        List<PendingUpload> expired = pendingRepo.findByStatusAndExpiresAtBefore(
                PendingUpload.Status.PENDING, Instant.now());

        if (expired.isEmpty()) {
            return;
        }

        for (PendingUpload pending : expired) {
            if (pending.isMultipart()) {
                try {
                    storage.abortMultipart(pending.getStorageKey(), pending.getMultipartUploadId());
                } catch (RuntimeException e) {
                    // Não interrompe a varredura: um multipart que já sumiu do bucket não
                    // pode impedir os outros registros de serem fechados.
                    log.warn("Falha ao abortar multipart de {}: {}",
                            pending.getStorageKey(), e.getMessage());
                }
            }

            pending.setStatus(PendingUpload.Status.EXPIRED);
        }

        pendingRepo.saveAll(expired);

        log.info("Varredura de uploads: {} registros expirados", expired.size());
    }
}
