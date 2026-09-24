package com.plantahub.api.shared.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Única classe da aplicação que fala com o SDK da AWS.
 *
 * <p>Consome os beans {@link S3Client} e {@link S3Presigner} declarados em
 * {@code S3Config}, em vez de construir clientes próprios — antes desta classe existiam
 * três clientes S3 no processo, com duas estratégias de credencial concorrentes.
 */
@Component
public class S3ObjectStorageAdapter implements ObjectStoragePort {

    private static final int LIST_PAGE_SIZE = 1000;

    private final String bucket;
    private final S3Client s3Client;
    private final S3Presigner presigner;

    public S3ObjectStorageAdapter(
            @Value("${app.s3.bucket}") String bucket,
            S3Client s3Client,
            S3Presigner presigner
    ) {
        this.bucket = bucket;
        this.s3Client = s3Client;
        this.presigner = presigner;
    }

    @Override
    public List<StoredObject> list(String prefix) {
        List<StoredObject> result = new ArrayList<>();
        String continuationToken = null;

        do {
            ListObjectsV2Request.Builder request = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(prefix)
                    .maxKeys(LIST_PAGE_SIZE);

            if (continuationToken != null) {
                request.continuationToken(continuationToken);
            }

            ListObjectsV2Response response = s3Client.listObjectsV2(request.build());

            response.contents().stream()
                    // marcadores de diretório não são arquivos
                    .filter(obj -> !obj.key().endsWith("/"))
                    .forEach(obj -> result.add(new StoredObject(
                            obj.key(),
                            obj.size(),
                            obj.lastModified(),
                            normalizeETag(obj.eTag()),
                            null // ListObjectsV2 não devolve content-type
                    )));

            continuationToken = response.nextContinuationToken();
        } while (continuationToken != null);

        return result;
    }

    @Override
    public Optional<StoredObject> head(String key) {
        try {
            HeadObjectResponse response = s3Client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build()
            );

            return Optional.of(new StoredObject(
                    key,
                    response.contentLength(),
                    response.lastModified(),
                    normalizeETag(response.eTag()),
                    response.contentType()
            ));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    @Override
    public InputStream open(String key) {
        return s3Client.getObject(
                GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build()
        );
    }

    @Override
    public void put(String key, Path file, String contentType) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromFile(file)
        );
    }

    @Override
    public String presignGet(String key, Duration ttl, String downloadFilename) {
        String filename = downloadFilename != null && !downloadFilename.isBlank()
                ? downloadFilename
                : lastSegment(key);

        return presigner.presignGetObject(builder ->
                builder.signatureDuration(ttl)
                        .getObjectRequest(req -> req
                                .bucket(bucket)
                                .key(key)
                                .responseContentDisposition("attachment; filename=\"" + filename + "\"")
                                .responseContentType("application/octet-stream")
                        )
        ).url().toString();
    }

    // ------------------------------------------------------------------
    // Escrita pelo navegador
    // ------------------------------------------------------------------

    @Override
    public String presignPut(String key, Duration ttl, String contentType) {
        return presigner.presignPutObject(builder ->
                builder.signatureDuration(ttl)
                        .putObjectRequest(req -> req
                                .bucket(bucket)
                                .key(key)
                                // Entra na assinatura: o navegador precisa enviar exatamente
                                // este cabecalho, ou a S3 recusa com SignatureDoesNotMatch.
                                .contentType(contentType)
                        )
        ).url().toString();
    }

    @Override
    public String initiateMultipart(String key, String contentType) {
        return s3Client.createMultipartUpload(CreateMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build()
        ).uploadId();
    }

    @Override
    public String presignUploadPart(String key, String uploadId, int partNumber, Duration ttl) {
        return presigner.presignUploadPart(builder ->
                builder.signatureDuration(ttl)
                        .uploadPartRequest(UploadPartRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .uploadId(uploadId)
                                .partNumber(partNumber)
                                .build())
        ).url().toString();
    }

    @Override
    public void completeMultipart(String key, String uploadId, List<PartETag> parts) {
        List<CompletedPart> completed = parts.stream()
                .sorted((a, b) -> Integer.compare(a.partNumber(), b.partNumber()))
                .map(part -> CompletedPart.builder()
                        .partNumber(part.partNumber())
                        .eTag(part.eTag())
                        .build())
                .toList();

        s3Client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .uploadId(uploadId)
                .multipartUpload(CompletedMultipartUpload.builder().parts(completed).build())
                .build());
    }

    @Override
    public void abortMultipart(String key, String uploadId) {
        s3Client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .uploadId(uploadId)
                .build());
    }

    /** O S3 devolve o ETag entre aspas; guardamos sem elas. */
    private String normalizeETag(String eTag) {
        if (eTag == null) {
            return null;
        }
        return eTag.replace("\"", "");
    }

    private String lastSegment(String key) {
        int idx = key.lastIndexOf('/');
        return idx >= 0 ? key.substring(idx + 1) : key;
    }
}
