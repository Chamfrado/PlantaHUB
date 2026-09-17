package com.plantahub.api.web.dto.admin;

import com.plantahub.api.domain.uploads.PendingUpload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.UUID;

public final class UploadDTOs {

    private UploadDTOs() {
    }

    public record PresignRequest(
            @NotNull PendingUpload.TargetKind targetKind,
            @NotBlank String productId,
            /** Obrigatório para ASSET; ignorado para MEDIA. */
            String collectionCode,
            @NotBlank String filename,
            String contentType,
            @NotNull @Positive Long sizeBytes,
            /** Caminho dentro da coleção, quando o arquivo veio de um upload de pasta. */
            String relativePath
    ) {}

    public record PresignedPart(int partNumber, String url) {}

    /**
     * Resposta do presign.
     *
     * <p>{@code method} diz ao cliente qual caminho seguir: um PUT único ou o fluxo
     * multipart. O cliente não escolhe — quem decide é o servidor, pelo tamanho declarado.
     */
    public record PresignResponse(
            UUID uploadId,
            String storageKey,
            String method,
            String url,
            /**
             * Cabeçalho que o navegador DEVE enviar, exatamente assim. Ele entra na
             * assinatura: qualquer diferença resulta em SignatureDoesNotMatch.
             */
            String contentType,
            String multipartUploadId,
            Long partSizeBytes,
            List<PresignedPart> parts,
            long expiresInSeconds
    ) {
        public static PresignResponse singlePut(UUID uploadId, String key, String url,
                                                String contentType, long expiresInSeconds) {
            return new PresignResponse(uploadId, key, "PUT", url, contentType,
                    null, null, null, expiresInSeconds);
        }

        public static PresignResponse multipart(UUID uploadId, String key, String multipartUploadId,
                                                long partSizeBytes, List<PresignedPart> parts,
                                                long expiresInSeconds) {
            return new PresignResponse(uploadId, key, "MULTIPART", null, null,
                    multipartUploadId, partSizeBytes, parts, expiresInSeconds);
        }
    }

    public record PresignBatchRequest(
            @NotBlank String productId,
            @NotNull PendingUpload.TargetKind targetKind,
            /** Usada quando a pasta do arquivo não corresponde a nenhuma coleção. */
            String defaultCollectionCode,
            @NotNull List<BatchFile> files
    ) {
        public record BatchFile(
                @NotBlank String filename,
                String contentType,
                @NotNull @Positive Long sizeBytes,
                /** Ex.: {@code ARCH/plantas/planta-01.dwg}. */
                String relativePath,
                /** Coleção resolvida pelo cliente a partir da tabela de mapeamento. */
                String collectionCode
        ) {}
    }

    public record MorePartsRequest(int fromPartNumber, int count) {}

    public record CompleteMultipartRequest(@NotNull List<Part> parts) {
        public record Part(int partNumber, @NotBlank String etag) {}
    }

    public record ConfirmUploadRequest(String checksumSha256, String kind) {}

    public record ConfirmUploadResponse(UUID id, String storageKey) {}
}
