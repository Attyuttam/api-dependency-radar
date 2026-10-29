
package com.apidependencyradar.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

public interface ApiDependencyRepository
        extends JpaRepository<ApiDependencyEntity, String> {

    // Existing methods; retained temporarily for compatibility.
    List<ApiDependencyEntity> findAllByOrderByCreatedAtDesc();

    boolean existsBySourceUrl(String sourceUrl);

    Optional<ApiDependencyEntity> findBySourceUrl(String sourceUrl);

    List<ApiDependencyEntity> findAllBySourceUrl(String sourceUrl);

    // New owner-scoped methods.
    List<ApiDependencyEntity> findAllByOwner_IdOrderByCreatedAtDesc(
            String ownerId);

    boolean existsByOwner_IdAndSourceUrl(
            String ownerId, String sourceUrl);

    Optional<ApiDependencyEntity> findByIdAndOwner_Id(
            String id, String ownerId);

    List<ApiDependencyEntity> findTop50ByNextCheckAtLessThanEqualOrderByNextCheckAtAsc(
            Instant now);

    @Query(value = """
            SELECT *
            FROM api_dependencies
            WHERE next_check_at <= :now
            ORDER BY next_check_at ASC
            LIMIT 50
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<ApiDependencyEntity> findDueDependenciesForUpdate(
            @Param("now") Instant now);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE api_dependencies
            SET monitoring_claim_token = :token,
                monitoring_claim_until = :claimUntil
            WHERE id = :id
              AND next_check_at <= :now
              AND (
                  monitoring_claim_until IS NULL
                  OR monitoring_claim_until <= :now
              )
            """, nativeQuery = true)
    int claimDependency(
            @Param("id") String id,
            @Param("token") String token,
            @Param("now") Instant now,
            @Param("claimUntil") Instant claimUntil);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE api_dependencies
            SET monitoring_claim_token = NULL,
                monitoring_claim_until = NULL
            WHERE id = :id
              AND monitoring_claim_token = :token
            """, nativeQuery = true)
    int releaseDependencyClaim(
            @Param("id") String id,
            @Param("token") String token);

    @Query(value = """
            SELECT COUNT(*) > 0
            FROM api_dependencies
            WHERE id = :id
              AND monitoring_claim_token = :token
              AND monitoring_claim_until > :now
            """, nativeQuery = true)
    boolean isClaimValid(
            @Param("id") String id,
            @Param("token") String token,
            @Param("now") Instant now);
}
