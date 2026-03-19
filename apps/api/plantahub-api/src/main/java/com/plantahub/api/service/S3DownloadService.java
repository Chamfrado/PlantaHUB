package com.plantahub.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.nio.file.Path;
import java.time.Duration;

@Service
public class S3DownloadService {

    private final String bucket;
    private final S3Presigner presigner;
    private final S3Client s3Client;

    public S3DownloadService(
            @Value("${app.s3.bucket}") String bucket,
            @Value("${app.s3.region}") String region,
            @Value("${app.s3.access-key}") String accessKey,
            @Value("${app.s3.secret-key}") String secretKey
    ) {
        this.bucket = bucket;

        var credentials = AwsBasicCredentials.create(accessKey, secretKey);
        var provider = StaticCredentialsProvider.create(credentials);
        var awsRegion = Region.of(region);

        this.presigner = S3Presigner.builder()
                .region(awsRegion)
                .credentialsProvider(provider)
                .build();

        this.s3Client = S3Client.builder()
                .region(awsRegion)
                .credentialsProvider(provider)
                .build();
    }

    public String generatePresignedUrl(String storageKey, Duration duration) {
        PresignedGetObjectRequest presigned = presigner.presignGetObject(builder ->
                builder.signatureDuration(duration)
                        .getObjectRequest(req -> req.bucket(bucket).key(storageKey))
        );
        return presigned.url().toString();
    }

    public void uploadFile(String storageKey, Path file, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .contentType(contentType)
                .build();

        s3Client.putObject(request, RequestBody.fromFile(file));
    }
}