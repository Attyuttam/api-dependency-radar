package com.apidependencyradar.web.dto;

import java.time.Instant;
import java.util.List;

public record CheckResultResponse(
        String dependencyId,
        Instant checkedAt,
        int changesDetected,
        int breakingChangesDetected,
        List<ApiChangeResponse> changes) {
}
