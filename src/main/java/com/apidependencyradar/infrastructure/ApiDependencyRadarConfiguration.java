package com.apidependencyradar.infrastructure;

import com.apidependencyradar.domain.detector.ApiChangeDetector;
import com.apidependencyradar.domain.detector.OpenApiChangeDetector;
import com.apidependencyradar.infrastructure.openapi.OpenApiSpecFetcher;
import com.apidependencyradar.infrastructure.openapi.OpenApiUrlValidator;
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
    OpenApiSpecFetcher openApiSpecFetcher(OpenApiUrlValidator validator) {
        return new OpenApiSpecFetcher(validator);
    }
}
