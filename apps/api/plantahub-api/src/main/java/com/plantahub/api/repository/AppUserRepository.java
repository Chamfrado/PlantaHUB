package com.plantahub.api.repository;

import com.plantahub.api.domain.auth.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    java.util.List<com.plantahub.api.domain.auth.AppUser> findByRole(
            com.plantahub.api.domain.auth.enums.UserRole role);

    long countByRole(com.plantahub.api.domain.auth.enums.UserRole role);

    Optional<AppUser> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<AppUser> findByCpf(String cpf);
    Optional<AppUser> findByEmailAndActiveTrueAndDeletedAtIsNull(String email);
    boolean existsByEmailAndActiveTrueAndDeletedAtIsNull(String email);
}
