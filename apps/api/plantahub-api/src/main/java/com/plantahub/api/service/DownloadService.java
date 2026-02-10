package com.plantahub.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.presigner.*;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

@Service
public class DownloadService {

    private final S3Presigner presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    public DownloadService(S3Presigner presigner) {
        this.presigner = presigner;
    }

    public String generatePresignedUrl(String storageKey) {
        System.out.println("S3 bucket in use: " + bucket);
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15)) // link válido por 15 min
                .getObjectRequest(getObjectRequest)
                .build();

        return presigner.presignGetObject(presignRequest).url().toString();
    }
}
