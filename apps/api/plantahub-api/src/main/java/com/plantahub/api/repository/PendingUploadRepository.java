package com.plantahub.api.repository;

import com.plantahub.api.domain.uploads.PendingUpload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PendingUploadRepository extends JpaRepository<PendingUpload, UUID> {

    List<PendingUpload> findByStatusAndExpiresAtBefore(PendingUpload.Status status, Instant cutoff);
}
