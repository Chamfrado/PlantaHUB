package com.plantahub.api.repository;

import com.plantahub.api.domain.config.AppSecret;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSecretRepository extends JpaRepository<AppSecret, String> {
}
