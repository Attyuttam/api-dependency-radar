
package com.apidependencyradar.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

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
}
