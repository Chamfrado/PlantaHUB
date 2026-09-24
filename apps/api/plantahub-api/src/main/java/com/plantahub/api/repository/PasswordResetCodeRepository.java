package com.plantahub.api.repository;

import com.plantahub.api.domain.auth.PasswordResetCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, UUID> {

    /** O pedido mais recente ainda aberto. So ele vale: pedir de novo invalida os anteriores. */
    Optional<PasswordResetCode> findFirstByUserIdAndVerifiedAtIsNullAndConsumedAtIsNullAndInvalidatedAtIsNullOrderByCreatedAtDesc(UUID userId);

    Optional<PasswordResetCode> findByResetTokenHash(String resetTokenHash);

    @Modifying
    @Query("""
            update PasswordResetCode c set c.invalidatedAt = :now
            where c.user.id = :userId and c.consumedAt is null and c.invalidatedAt is null
            """)
    int invalidateOpenForUser(@Param("userId") UUID userId, @Param("now") Instant now);

    @Modifying
    @Query("delete from PasswordResetCode c where c.createdAt < :cutoff")
    int deleteCreatedBefore(@Param("cutoff") Instant cutoff);
}
