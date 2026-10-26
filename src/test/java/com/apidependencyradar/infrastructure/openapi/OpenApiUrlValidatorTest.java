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
                                () -> validator.validate("http://api.example.com/openapi.yaml"));
        }

        @Test
        void shouldRejectLocalhost() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> validator.validate("https://localhost/openapi.yaml"));
        }

        @Test
        void shouldRejectLoopbackAddress() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> validator.validate("https://127.0.0.1/openapi.yaml"));
        }

        @Test
        void shouldRejectPrivateAddress() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> validator.validate("https://192.168.1.10/openapi.yaml"));
        }

        @Test
        void shouldRejectInvalidUrl() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> validator.validate("not-a-url"));
        }

        @Test
        void shouldRejectIpv6Loopback() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> validator.validate("https://[::1]/openapi.yaml"));
        }

        @Test
        void shouldRejectIpv6UniqueLocalAddress() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> validator.validate("https://[fc00::1]/openapi.yaml"));
        }

        @Test
        void shouldRejectIpv6DocumentationAddress() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> validator.validate("https://[3fff::1]/openapi.yaml"));
        }

        @Test
        void shouldRejectLinkLocalIpv4() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> validator.validate("https://169.254.169.254/openapi.yaml"));
        }

        @Test
        void shouldRejectCarrierGradeNatAddress() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> validator.validate("https://100.64.0.1/openapi.yaml"));
        }

        @Test
        void shouldRejectDocumentationAddress() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> validator.validate("https://192.0.2.1/openapi.yaml"));
        }

        @Test
        void shouldRejectUrlContainingUserInformation() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> validator.validate("https://user:password@1.1.1.1/openapi.yaml"));
        }

        @Test
        void shouldRejectInvalidPort() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> validator.validate("https://1.1.1.1:99999/openapi.yaml"));
        }

        @Test
        void shouldRejectBlankUrl() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> validator.validate(" "));
        }

        @Test
        void shouldRejectIpv6ProtocolAssignmentsRange() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> validator.validate("https://[2001::1]/openapi.yaml"));
        }

        @Test
        void shouldRejectIpv6OrchidRange() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> validator.validate("https://[2001:10::1]/openapi.yaml"));
        }

        @Test
        void shouldRejectIpv6OrchidV2Range() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> validator.validate("https://[2001:20::1]/openapi.yaml"));
        }

        @Test
        void shouldAllowPublicIpv6Address() {
                assertDoesNotThrow(
                                () -> validator.validate("https://[2606:4700:4700::1111]/openapi.yaml"));
        }

}