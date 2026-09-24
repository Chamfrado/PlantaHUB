package com.plantahub.api.shared.storage;

import java.time.Instant;

/**
 * Um objeto que existe fisicamente no bucket.
 *
 * <p>{@code contentType} vem preenchido apenas em {@link ObjectStorageReader#head(String)}:
 * a listagem do S3 (ListObjectsV2) não devolve esse metadado, então em resultados de
 * {@link ObjectStorageReader#list(String)} ele é sempre {@code null}.
 */
public record StoredObject(
        String key,
        Long sizeBytes,
        Instant lastModified,
        String eTag,
        String contentType
) {
}
