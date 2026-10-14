package com.apidependencyradar.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApiChangeRepository extends JpaRepository<ApiChangeEntity, String> {
    List<ApiChangeEntity> findByDependency_IdOrderByDetectedAtDesc(String dependencyId);
    long countByDependency_Id(String dependencyId);
    long countByDependency_IdAndBreakingTrue(String dependencyId);
}
