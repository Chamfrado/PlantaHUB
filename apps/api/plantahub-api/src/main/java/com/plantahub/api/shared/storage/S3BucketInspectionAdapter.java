package com.plantahub.api.shared.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetBucketCorsRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLifecycleConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Implementação S3 da inspeção do bucket.
 *
 * <p>Junto com {@link S3ObjectStorageAdapter}, é o único lugar que conhece o SDK da AWS.
 */
@Component
public class S3BucketInspectionAdapter implements BucketInspectionPort {

    /**
     * Sem credencial, sem cookie e sem seguir redirecionamento.
     *
     * <p>Seguir um 301 da S3 mascararia justamente o que interessa: um redirecionamento
     * para o endpoint regional certo não é a mesma coisa que o objeto ser legível.
     */
    private static final HttpClient ANONYMOUS = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private final S3Client s3;
    private final String bucket;

    public S3BucketInspectionAdapter(S3Client s3, @Value("${app.s3.bucket}") String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    @Override
    public boolean bucketExists() {
        try {
            s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            return true;
        } catch (NoSuchBucketException e) {
            return false;
        } catch (AwsServiceException e) {
            // 404 tambem chega como erro generico dependendo da permissao de ListBucket.
            if (e.statusCode() == 404) {
                return false;
            }
            throw unavailable("HeadBucket", e);
        } catch (RuntimeException e) {
            throw unavailable("HeadBucket", e);
        }
    }

    @Override
    public String bucketRegion() {
        try {
            var response = s3.getBucketLocation(
                    GetBucketLocationRequest.builder().bucket(bucket).build());

            String constraint = response.locationConstraintAsString();

            // A S3 devolve vazio para us-east-1 por razoes historicas; traduzir aqui evita
            // um "regiao divergente" falso no diagnostico.
            return constraint == null || constraint.isBlank() ? "us-east-1" : constraint;
        } catch (RuntimeException e) {
            throw unavailable("GetBucketLocation", e);
        }
    }

    @Override
    public List<CorsRule> corsRules() {
        try {
            return s3.getBucketCors(GetBucketCorsRequest.builder().bucket(bucket).build())
                    .corsRules()
                    .stream()
                    .map(rule -> new CorsRule(
                            rule.allowedOrigins(),
                            rule.allowedMethods(),
                            rule.allowedHeaders(),
                            rule.exposeHeaders()))
                    .toList();
        } catch (AwsServiceException e) {
            // Bucket sem CORS nenhum responde com este erro; e uma resposta, nao uma falha.
            if ("NoSuchCORSConfiguration".equals(e.awsErrorDetails().errorCode())) {
                return List.of();
            }
            throw unavailable("GetBucketCors", e);
        } catch (RuntimeException e) {
            throw unavailable("GetBucketCors", e);
        }
    }

    @Override
    public List<LifecycleRule> lifecycleRules() {
        try {
            return s3.getBucketLifecycleConfiguration(
                            GetBucketLifecycleConfigurationRequest.builder().bucket(bucket).build())
                    .rules()
                    .stream()
                    .map(rule -> new LifecycleRule(
                            rule.id(),
                            "Enabled".equalsIgnoreCase(rule.statusAsString()),
                            prefixOf(rule),
                            rule.abortIncompleteMultipartUpload() == null
                                    ? null
                                    : rule.abortIncompleteMultipartUpload().daysAfterInitiation()))
                    .toList();
        } catch (AwsServiceException e) {
            if ("NoSuchLifecycleConfiguration".equals(e.awsErrorDetails().errorCode())) {
                return List.of();
            }
            throw unavailable("GetBucketLifecycleConfiguration", e);
        } catch (RuntimeException e) {
            throw unavailable("GetBucketLifecycleConfiguration", e);
        }
    }

    @Override
    public AnonymousFetch fetchWithoutCredentials(String url) {
        try {
            var request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .method("GET", HttpRequest.BodyPublishers.noBody())
                    // Range minimo: basta saber se a S3 entrega, sem baixar o arquivo. HEAD
                    // nao serve — a politica pode liberar GetObject sem liberar HEAD.
                    .header("Range", "bytes=0-0")
                    .build();

            var response = ANONYMOUS.send(request, HttpResponse.BodyHandlers.discarding());
            return new AnonymousFetch(response.statusCode(), null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new AnonymousFetch(-1, "requisição interrompida");
        } catch (Exception e) {
            return new AnonymousFetch(-1, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /** O prefixo pode vir direto ou embrulhado num filtro; as duas formas são válidas. */
    private static String prefixOf(software.amazon.awssdk.services.s3.model.LifecycleRule rule) {
        if (rule.filter() == null) {
            return "";
        }

        if (rule.filter().prefix() != null) {
            return rule.filter().prefix();
        }

        return rule.filter().and() != null && rule.filter().and().prefix() != null
                ? rule.filter().and().prefix()
                : "";
    }

    private InspectionUnavailableException unavailable(String operation, Exception cause) {
        return new InspectionUnavailableException(operation + " falhou: " + cause.getMessage());
    }
}
