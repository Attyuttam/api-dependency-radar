package com.apidependencyradar.web.dto;

import java.time.Instant;

public record ApiDependencyResponse(
        String id,
        String name,
        String providerName,
        String sourceUrl,
        String sourceType,
        String description,
        String status,
        Instant lastCheckedAt,
        Instant createdAt,
        Instant updatedAt,
        long changeCount,
        long breakingChangeCount) {
}
