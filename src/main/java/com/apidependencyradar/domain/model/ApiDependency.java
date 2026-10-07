package com.apidependencyradar.domain.model;

public record ApiDependency(
        String id,
        String name,
        String sourceUrl,
        String sourceType,
        String baselineSpec) {
}