package com.apidependencyradar.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apidependencyradar.infrastructure.persistence.ApiDependencyEntity;
import com.apidependencyradar.infrastructure.persistence.ApiDependencyRepository;

import java.time.Duration;
import java.time.Instant;

@Service
public class ScheduledCheckPersistenceService {

    private final ApiDependencyRepository dependencyRepository;

    public ScheduledCheckPersistenceService(
            ApiDependencyRepository dependencyRepository) {
        this.dependencyRepository = dependencyRepository;
    }

    @Transactional
    public boolean recordFailureIfClaimValid(
            String dependencyId,
            String token,
            RuntimeException exception) {

        Instant now = Instant.now();

        boolean claimValid = dependencyRepository.isClaimValid(
                dependencyId,
                token,
                now);

        if (!claimValid) {
            return false;
        }

        ApiDependencyEntity dependency = dependencyRepository.findById(dependencyId).orElse(null);

        if (dependency == null) {
            return false;
        }

        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        } else {
            message = exception.getClass().getSimpleName()
                    + ": " + message;
        }

        if (message.length() > 1000) {
            message = message.substring(0, 1000);
        }

        dependency.markCheckFailed(
                message,
                now.plus(Duration.ofMinutes(15)));

        dependencyRepository.save(dependency);
        return true;
    }
}