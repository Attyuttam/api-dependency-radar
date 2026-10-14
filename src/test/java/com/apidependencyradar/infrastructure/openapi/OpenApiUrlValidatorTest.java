package com.apidependencyradar.infrastructure.openapi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenApiUrlValidatorTest {

    private final OpenApiUrlValidator validator = new OpenApiUrlValidator();

    @Test
    void shouldAllowValidHttpsUrl() {

        assertDoesNotThrow(() -> validator.validate("https://1.1.1.1/openapi.yaml"));
    }

    @Test
    void shouldRejectHttpUrl() {

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(
                        "http://api.example.com/openapi.yaml"));
    }

    @Test
    void shouldRejectLocalhost() {

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(
                        "https://localhost/openapi.yaml"));
    }

    @Test
    void shouldRejectLoopbackAddress() {

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(
                        "https://127.0.0.1/openapi.yaml"));
    }

    @Test
    void shouldRejectPrivateAddress() {

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(
                        "https://192.168.1.10/openapi.yaml"));
    }

    @Test
    void shouldRejectInvalidUrl() {

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(
                        "not-a-url"));
    }
}