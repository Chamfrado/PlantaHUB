package com.plantahub.api.web.controller.admin;

import com.plantahub.api.service.admin.UploadService;
import com.plantahub.api.web.dto.admin.UploadDTOs.CompleteMultipartRequest;
import com.plantahub.api.web.dto.admin.UploadDTOs.ConfirmUploadRequest;
import com.plantahub.api.web.dto.admin.UploadDTOs.ConfirmUploadResponse;
import com.plantahub.api.web.dto.admin.UploadDTOs.MorePartsRequest;
import com.plantahub.api.web.dto.admin.UploadDTOs.PresignBatchRequest;
import com.plantahub.api.web.dto.admin.UploadDTOs.PresignRequest;
import com.plantahub.api.web.dto.admin.UploadDTOs.PresignResponse;
import com.plantahub.api.web.dto.admin.UploadDTOs.PresignedPart;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Upload direto do navegador para o bucket.
 *
 * <p>A API assina URLs e registra o resultado; os bytes nunca passam por aqui. Enviar
 * arquivos de centenas de megabytes por dentro da aplicação consumiria memória e tempo de
 * requisição sem nenhum ganho.
 */
@RestController
@RequestMapping("/v1/admin/uploads")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUploadController {

    private final UploadService uploadService;

    public AdminUploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping("/presign")
    public PresignResponse presign(@AuthenticationPrincipal UserDetails user,
                                   @Valid @RequestBody PresignRequest request) {
        return uploadService.presign(user.getUsername(), request);
    }

    /**
     * Autoriza vários arquivos de uma vez (upload de pasta).
     *
     * <p>Sem transação abrangendo o lote, de propósito: um arquivo recusado não pode
     * derrubar os outros duzentos. Cada entrada devolve seu próprio resultado.
     */
    @PostMapping("/presign-batch")
    public List<PresignResponse> presignBatch(@AuthenticationPrincipal UserDetails user,
                                              @Valid @RequestBody PresignBatchRequest request) {
        List<PresignResponse> results = new ArrayList<>();

        for (var file : request.files()) {
            String collectionCode = file.collectionCode() != null && !file.collectionCode().isBlank()
                    ? file.collectionCode()
                    : request.defaultCollectionCode();

            results.add(uploadService.presign(user.getUsername(), new PresignRequest(
                    request.targetKind(),
                    request.productId(),
                    collectionCode,
                    file.filename(),
                    file.contentType(),
                    file.sizeBytes(),
                    file.relativePath()
            )));
        }

        return results;
    }

    @PostMapping("/{uploadId}/parts")
    public List<PresignedPart> moreParts(@PathVariable UUID uploadId,
                                         @RequestBody MorePartsRequest request) {
        return uploadService.moreParts(uploadId, request.fromPartNumber(), request.count());
    }

    @PostMapping("/{uploadId}/complete-multipart")
    public ResponseEntity<Void> completeMultipart(@PathVariable UUID uploadId,
                                                  @Valid @RequestBody CompleteMultipartRequest request) {
        uploadService.completeMultipart(uploadId, request);
        return ResponseEntity.noContent().build();
    }

    /** Registra o arquivo, com tamanho e tipo lidos do próprio bucket. */
    @PostMapping("/{uploadId}/confirm")
    public ConfirmUploadResponse confirm(@PathVariable UUID uploadId,
                                         @RequestBody(required = false) ConfirmUploadRequest request) {
        return uploadService.confirmWithUrl(uploadId, request);
    }

    @DeleteMapping("/{uploadId}")
    public ResponseEntity<Void> abort(@PathVariable UUID uploadId) {
        uploadService.abort(uploadId);
        return ResponseEntity.noContent().build();
    }
}
