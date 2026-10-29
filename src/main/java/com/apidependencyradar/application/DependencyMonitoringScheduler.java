package com.apidependencyradar.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.apidependencyradar.infrastructure.persistence.ApiDependencyRepository;

import java.time.Instant;

@Component
public class DependencyMonitoringScheduler {

    private static final Logger log = LoggerFactory.getLogger(DependencyMonitoringScheduler.class);

    private final ApiDependencyRepository dependencyRepository;
    private final ApiDependencyService dependencyService;

    public DependencyMonitoringScheduler(
            ApiDependencyRepository dependencyRepository,
            ApiDependencyService dependencyService) {
        this.dependencyRepository = dependencyRepository;
        this.dependencyService = dependencyService;
    }

    @Scheduled(fixedDelayString = "${app.monitoring.poll-interval-ms:60000}")
    public void monitorDueDependencies() {
        var dueDependencies = dependencyRepository
                .findTop50ByNextCheckAtLessThanEqualOrderByNextCheckAtAsc(
                        Instant.now());

        for (var dependency : dueDependencies) {
            try {
                dependencyService.checkScheduled(dependency.getId());
            } catch (RuntimeException ex) {
                log.error(
                        "Scheduled monitoring failed for dependency {}",
                        dependency.getId(),
                        ex);
            }
        }
    }
}
