package com.apidependencyradar.infrastructure;

import com.apidependencyradar.domain.detector.ApiChangeDetector;
import com.apidependencyradar.domain.detector.OpenApiChangeDetector;
import com.apidependencyradar.infrastructure.openapi.OpenApiSpecFetcher;
import com.apidependencyradar.infrastructure.openapi.OpenApiUrlValidator;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiDependencyRadarConfiguration {

    @Bean
    ApiChangeDetector apiChangeDetector() {
        return new OpenApiChangeDetector();
    }

    @Bean
    OpenApiUrlValidator openApiUrlValidator() {
        return new OpenApiUrlValidator();
    }

    @Bean
    OpenApiSpecFetcher openApiSpecFetcher(
            OpenApiUrlValidator validator,
            @Value("${app.openapi.max-response-size-bytes:20971520}") int maxResponseSizeBytes,
            @Value("${app.openapi.max-fetch-duration:30s}") Duration maxFetchDuration) {

        return new OpenApiSpecFetcher(
                validator,
                maxResponseSizeBytes,
                maxFetchDuration);
    }
}
