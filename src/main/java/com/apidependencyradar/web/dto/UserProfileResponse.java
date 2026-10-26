package com.apidependencyradar.web.dto;

import java.time.Instant;

public record UserProfileResponse(
        String id,
        String email,
        Instant createdAt) {
}
