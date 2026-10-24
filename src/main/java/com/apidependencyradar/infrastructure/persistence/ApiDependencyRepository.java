package com.apidependencyradar.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiDependencyRepository extends JpaRepository<ApiDependencyEntity, String> {

    List<ApiDependencyEntity> findAllByOrderByCreatedAtDesc();

    boolean existsBySourceUrl(String sourceUrl);

    Optional<ApiDependencyEntity> findBySourceUrl(String sourceUrl);

    List<ApiDependencyEntity> findAllBySourceUrl(String sourceUrl);
}