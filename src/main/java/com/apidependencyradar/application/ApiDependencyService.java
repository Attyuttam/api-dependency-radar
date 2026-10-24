
package com.apidependencyradar.application;

import com.apidependencyradar.domain.detector.ApiChangeDetector;
import com.apidependencyradar.domain.model.ApiChange;
import com.apidependencyradar.infrastructure.openapi.OpenApiSpecFetcher;
import com.apidependencyradar.infrastructure.persistence.ApiChangeEntity;
import com.apidependencyradar.infrastructure.persistence.ApiChangeRepository;
import com.apidependencyradar.infrastructure.persistence.ApiDependencyEntity;
import com.apidependencyradar.infrastructure.persistence.ApiDependencyRepository;
import com.apidependencyradar.infrastructure.persistence.AppUserEntity;
import com.apidependencyradar.infrastructure.persistence.AppUserRepository;
import com.apidependencyradar.web.dto.ApiChangeResponse;
import com.apidependencyradar.web.dto.ApiDependencyResponse;
import com.apidependencyradar.web.dto.CheckResultResponse;
import com.apidependencyradar.web.dto.CreateApiDependencyRequest;
import com.apidependencyradar.web.error.ResourceNotFoundException;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ApiDependencyService {

    private static final String SOURCE_TYPE = "OPENAPI";
    private static final String OWNER_SOURCE_CONSTRAINT = "uk_api_dependencies_owner_source_url";

    private final ApiDependencyRepository dependencyRepository;
    private final ApiChangeRepository changeRepository;
    private final AppUserRepository userRepository;
    private final OpenApiSpecFetcher specFetcher;
    private final ApiChangeDetector changeDetector;

    public ApiDependencyService(
            ApiDependencyRepository dependencyRepository,
            ApiChangeRepository changeRepository,
            AppUserRepository userRepository,
            OpenApiSpecFetcher specFetcher,
            ApiChangeDetector changeDetector) {
        this.dependencyRepository = dependencyRepository;
        this.changeRepository = changeRepository;
        this.userRepository = userRepository;
        this.specFetcher = specFetcher;
        this.changeDetector = changeDetector;
    }

    @Transactional
    public ApiDependencyResponse create(CreateApiDependencyRequest request) {
        AppUserEntity owner = currentUser();
        String sourceUrl = request.sourceUrl().trim();

        if (dependencyRepository.existsByOwner_IdAndSourceUrl(
                owner.getId(), sourceUrl)) {
            throw new DuplicateApiDependencyException(
                    "An API dependency with this source URL already exists");
        }

        String specification = specFetcher.fetch(sourceUrl);
        validateSpecification(specification);

        ApiDependencyEntity entity = new ApiDependencyEntity(
                UUID.randomUUID().toString(),
                owner,
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
            if (isOwnerSourceConstraintViolation(ex)) {
                throw new DuplicateApiDependencyException(
                        "An API dependency with this source URL already exists");
            }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<ApiDependencyResponse> list() {
        AppUserEntity owner = currentUser();

        return dependencyRepository
                .findAllByOwner_IdOrderByCreatedAtDesc(owner.getId())
                .stream()
                .map(this::toDependencyResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApiDependencyResponse get(String id) {
        AppUserEntity owner = currentUser();
        return toDependencyResponse(findDependency(id, owner));
    }

    @Transactional
    public void delete(String id) {
        AppUserEntity owner = currentUser();
        ApiDependencyEntity dependency = findDependency(id, owner);

        List<ApiChangeEntity> changes = changeRepository.findByDependency_IdOrderByDetectedAtDesc(
                dependency.getId());

        changeRepository.deleteAll(changes);
        dependencyRepository.delete(dependency);
    }

    @Transactional(readOnly = true)
    public List<ApiChangeResponse> listChanges(String id) {
        AppUserEntity owner = currentUser();
        ApiDependencyEntity dependency = findDependency(id, owner);

        return changeRepository
                .findByDependency_IdOrderByDetectedAtDesc(dependency.getId())
                .stream()
                .map(change -> toChangeResponse(change, dependency.getId()))
                .toList();
    }

    @Transactional
    public CheckResultResponse checkNow(String id) {
        AppUserEntity owner = currentUser();
        ApiDependencyEntity dependency = findDependency(id, owner);

        String currentSpecification = specFetcher.fetch(dependency.getSourceUrl());

        List<ApiChange> detectedChanges;
        try {
            detectedChanges = changeDetector.detect(
                    dependency.getBaselineSpec(), currentSpecification);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "The source returned an invalid OpenAPI specification", ex);
        }

        Instant checkedAt = Instant.now();

        List<ApiChangeEntity> newChangeEntities = detectedChanges.stream()
                .map(change -> new ApiChangeEntity(
                        UUID.randomUUID().toString(),
                        dependency,
                        change.sourceType(),
                        change.type(),
                        change.endpoint(),
                        change.description(),
                        change.breaking()))
                .toList();

        List<ApiChangeEntity> persistedChanges = changeRepository.saveAll(newChangeEntities);

        dependency.updateBaseline(currentSpecification);
        dependency.markChecked(checkedAt);
        dependencyRepository.save(dependency);

        List<ApiChangeResponse> currentRunChanges = persistedChanges.stream()
                .map(change -> toChangeResponse(change, dependency.getId()))
                .toList();

        int breakingCount = (int) detectedChanges.stream()
                .filter(ApiChange::breaking)
                .count();

        return new CheckResultResponse(
                dependency.getId(),
                checkedAt,
                detectedChanges.size(),
                breakingCount,
                currentRunChanges);
    }

    private AppUserEntity currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new UsernameNotFoundException(
                    "An authenticated user is required");
        }

        return userRepository.findByEmailIgnoreCase(
                authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Authenticated account no longer exists"));
    }

    private ApiDependencyEntity findDependency(
            String id, AppUserEntity owner) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                    "Dependency ID must not be blank");
        }

        return dependencyRepository.findByIdAndOwner_Id(
                id.trim(), owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "API dependency not found"));
    }

    private boolean isOwnerSourceConstraintViolation(Throwable exception) {
        Throwable current = exception;

        while (current != null) {
            if (current instanceof ConstraintViolationException violation
                    && OWNER_SOURCE_CONSTRAINT.equals(
                            violation.getConstraintName())) {
                return true;
            }
            current = current.getCause();
        }

        return false;
    }

    private void validateSpecification(String specification) {
        try {
            changeDetector.detect(specification, specification);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "The URL did not return a valid OpenAPI specification", ex);
        }
    }

    private ApiDependencyResponse toDependencyResponse(
            ApiDependencyEntity entity) {
        return new ApiDependencyResponse(
                entity.getId(),
                entity.getName(),
                entity.getProviderName(),
                entity.getSourceUrl(),
                entity.getSourceType(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getLastCheckedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                changeRepository.countByDependency_Id(entity.getId()),
                changeRepository.countByDependency_IdAndBreakingTrue(
                        entity.getId()));
    }

    private ApiChangeResponse toChangeResponse(
            ApiChangeEntity entity, String dependencyId) {
        return new ApiChangeResponse(
                entity.getId(),
                dependencyId,
                entity.getSourceType(),
                entity.getType(),
                entity.getEndpoint(),
                entity.getDescription(),
                entity.isBreaking(),
                entity.getDetectedAt());
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
