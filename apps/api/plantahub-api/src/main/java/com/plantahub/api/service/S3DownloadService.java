package com.plantahub.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URL;
import java.time.Duration;

@Service
public class S3DownloadService {

    private final S3Presigner presigner;
    private final String bucket;

    public S3DownloadService(S3Presigner presigner,
                             @Value("${aws.s3.bucket}") String bucket) {
        this.presigner = presigner;
        this.bucket = bucket;
    }

    public String generatePresignedUrl(String key, Duration expires) {
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        GetObjectPresignRequest pre = GetObjectPresignRequest.builder()
                .signatureDuration(expires)
                .getObjectRequest(get)
                .build();

        URL url = presigner.presignGetObject(pre).url();
        return url.toString();
    }
}