package com.apidependencyradar.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateApiDependencyRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 120) String providerName,
        @NotBlank @Size(max = 2048) String sourceUrl,
        @Size(max = 1000) String description) {
}
