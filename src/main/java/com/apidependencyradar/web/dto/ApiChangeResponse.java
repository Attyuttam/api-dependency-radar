package com.apidependencyradar.web.dto;

import java.time.Instant;

public record ApiChangeResponse(
        String id,
        String dependencyId,
        String sourceType,
        String type,
        String endpoint,
        String description,
        boolean breaking,
        Instant detectedAt) {
}
