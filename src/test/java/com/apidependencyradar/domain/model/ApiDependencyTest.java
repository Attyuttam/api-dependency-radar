package com.apidependencyradar.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiDependencyTest {

    @Test
    void shouldCreateApiDependency() {

        ApiDependency dependency = new ApiDependency(
                "dep-1",
                "Stripe API",
                "https://example.com/openapi.yaml",
                "OPENAPI",
                "openapi: 3.0.0");

        assertEquals("dep-1", dependency.id());
        assertEquals("Stripe API", dependency.name());
        assertEquals(
                "https://example.com/openapi.yaml",
                dependency.sourceUrl());
        assertEquals("OPENAPI", dependency.sourceType());
        assertEquals("openapi: 3.0.0", dependency.baselineSpec());
    }
}