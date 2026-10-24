package com.apidependencyradar.application;

import com.apidependencyradar.domain.detector.ApiChangeDetector;
import com.apidependencyradar.domain.model.ApiChange;
import com.apidependencyradar.infrastructure.openapi.OpenApiSpecFetcher;
import com.apidependencyradar.infrastructure.persistence.ApiChangeEntity;
import com.apidependencyradar.infrastructure.persistence.ApiChangeRepository;
import com.apidependencyradar.infrastructure.persistence.ApiDependencyEntity;
import com.apidependencyradar.infrastructure.persistence.ApiDependencyRepository;
import com.apidependencyradar.web.dto.ApiChangeResponse;
import com.apidependencyradar.web.dto.ApiDependencyResponse;
import com.apidependencyradar.web.dto.CheckResultResponse;
import com.apidependencyradar.web.dto.CreateApiDependencyRequest;
import com.apidependencyradar.web.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ApiDependencyService {

    private static final String SOURCE_TYPE = "OPENAPI";

    private final ApiDependencyRepository dependencyRepository;
    private final ApiChangeRepository changeRepository;
    private final OpenApiSpecFetcher specFetcher;
    private final ApiChangeDetector changeDetector;

    public ApiDependencyService(ApiDependencyRepository dependencyRepository,
            ApiChangeRepository changeRepository,
            OpenApiSpecFetcher specFetcher,
            ApiChangeDetector changeDetector) {
        this.dependencyRepository = dependencyRepository;
        this.changeRepository = changeRepository;
        this.specFetcher = specFetcher;
        this.changeDetector = changeDetector;
    }

    @Transactional
    public ApiDependencyResponse create(CreateApiDependencyRequest request) {
        String sourceUrl = request.sourceUrl().trim();

        if (dependencyRepository.existsBySourceUrl(sourceUrl)) {
            throw new DuplicateApiDependencyException(
                    "An API dependency with this source URL already exists");
        }

        String specification = specFetcher.fetch(sourceUrl);
        validateSpecification(specification);

        ApiDependencyEntity entity = new ApiDependencyEntity(
                UUID.randomUUID().toString(),
                request.name().trim(),
                normalizeOptional(request.providerName()),
                sourceUrl,
                SOURCE_TYPE,
                normalizeOptional(request.description()),
                specification);

        try {
            ApiDependencyEntity saved = dependencyRepository.saveAndFlush(entity);
            return toDependencyResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            if (isSourceUrlUniqueConstraintViolation(ex)) {
                throw new DuplicateApiDependencyException(
                        "An API dependency with this source URL already exists");
            }

            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<ApiDependencyResponse> list() {
        return dependencyRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDependencyResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApiDependencyResponse get(String id) {
        return toDependencyResponse(findDependency(id));
    }

    @Transactional
    public void delete(String id) {
        ApiDependencyEntity dependency = findDependency(id);
        List<ApiChangeEntity> changes = changeRepository.findByDependency_IdOrderByDetectedAtDesc(id);
        changeRepository.deleteAll(changes);
        dependencyRepository.delete(dependency);
    }

    @Transactional(readOnly = true)
    public List<ApiChangeResponse> listChanges(String id) {
        findDependency(id);
        return changeRepository.findByDependency_IdOrderByDetectedAtDesc(id).stream()
                .map(change -> toChangeResponse(change, id))
                .toList();
    }

    @Transactional
    public CheckResultResponse checkNow(String id) {
        ApiDependencyEntity dependency = findDependency(id);
        String currentSpecification = specFetcher.fetch(dependency.getSourceUrl());
        List<ApiChange> detectedChanges;
        try {
            detectedChanges = changeDetector.detect(dependency.getBaselineSpec(), currentSpecification);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("The source returned an invalid OpenAPI specification", ex);
        }

        Instant checkedAt = Instant.now();
        List<ApiChangeEntity> newChangeEntities = detectedChanges.stream()
                .map(change -> new ApiChangeEntity(
                        UUID.randomUUID().toString(), dependency, change.sourceType(), change.type(),
                        change.endpoint(), change.description(), change.breaking()))
                .toList();
        List<ApiChangeEntity> persistedChanges = changeRepository.saveAll(newChangeEntities);

        dependency.updateBaseline(currentSpecification);
        dependency.markChecked(checkedAt);
        dependencyRepository.save(dependency);

        // Return only changes from this check; historical entries remain available via
        // the changes endpoint.
        List<ApiChangeResponse> currentRunChanges = persistedChanges.stream()
                .map(change -> toChangeResponse(change, id))
                .toList();
        int breakingCount = (int) detectedChanges.stream().filter(ApiChange::breaking).count();
        return new CheckResultResponse(id, checkedAt, detectedChanges.size(), breakingCount, currentRunChanges);
    }

    private boolean isSourceUrlUniqueConstraintViolation(Throwable exception) {
        Throwable current = exception;

        while (current != null) {
            if (current instanceof org.hibernate.exception.ConstraintViolationException violation) {
                if ("uk_api_dependencies_source_url".equals(
                        violation.getConstraintName())) {
                    return true;
                }
            }

            current = current.getCause();
        }

        return false;
    }

    private void validateSpecification(String specification) {
        try {
            changeDetector.detect(specification, specification);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("The URL did not return a valid OpenAPI specification", ex);
        }
    }

    private ApiDependencyEntity findDependency(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Dependency ID must not be blank");
        }
        return dependencyRepository.findById(id.trim())
                .orElseThrow(() -> new ResourceNotFoundException("API dependency not found: " + id));
    }

    private ApiDependencyResponse toDependencyResponse(ApiDependencyEntity entity) {
        return new ApiDependencyResponse(
                entity.getId(), entity.getName(), entity.getProviderName(), entity.getSourceUrl(),
                entity.getSourceType(), entity.getDescription(), entity.getStatus(),
                entity.getLastCheckedAt(), entity.getCreatedAt(), entity.getUpdatedAt(),
                changeRepository.countByDependency_Id(entity.getId()),
                changeRepository.countByDependency_IdAndBreakingTrue(entity.getId()));
    }

    private ApiChangeResponse toChangeResponse(ApiChangeEntity entity, String dependencyId) {
        return new ApiChangeResponse(entity.getId(), dependencyId, entity.getSourceType(), entity.getType(),
                entity.getEndpoint(), entity.getDescription(), entity.isBreaking(), entity.getDetectedAt());
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank())
            return null;
        return value.trim();
    }
}
