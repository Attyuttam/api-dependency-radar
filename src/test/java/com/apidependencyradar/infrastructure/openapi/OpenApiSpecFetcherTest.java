package com.apidependencyradar.infrastructure.openapi;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenApiSpecFetcherTest {

        private HttpServer server;

        @BeforeEach
        void setUp() throws IOException {

                server = HttpServer.create(
                                new InetSocketAddress(0),
                                0);

                server.createContext(
                                "/openapi.yaml",
                                exchange -> {

                                        String response = """
                                                        openapi: 3.0.0
                                                        info:
                                                          title: Test API
                                                          version: 1.0.0
                                                        paths: {}
                                                        """;

                                        exchange.sendResponseHeaders(
                                                        200,
                                                        response.getBytes().length);

                                        try (var outputStream = exchange.getResponseBody()) {
                                                outputStream.write(response.getBytes());
                                        }
                                });

                server.start();
        }

        @AfterEach
        void tearDown() {
                server.stop(0);
        }

        @Test
        void shouldFetchOpenApiSpecification() {

                OpenApiSpecFetcher fetcher = new OpenApiSpecFetcher(new OpenApiUrlValidator(true, true),
                                20 * 1024 * 1024, Duration.ofSeconds(30));
                String url = "http://localhost:"
                                + server.getAddress().getPort()
                                + "/openapi.yaml";

                String specification = fetcher.fetch(url);

                assertEquals(
                                "3.0.0",
                                specification
                                                .lines()
                                                .filter(line -> line.trim().startsWith("openapi:"))
                                                .map(line -> line.substring(line.indexOf(":") + 1).trim())
                                                .findFirst()
                                                .orElseThrow());
        }

        @Test
        void shouldRejectResponseExceedingMaximumSize() throws IOException {

                server.createContext(
                                "/large-openapi.yaml",
                                exchange -> {
                                        byte[] response = new byte[20 * 1024 * 1024 + 1];

                                        exchange.sendResponseHeaders(200, response.length);

                                        try (var outputStream = exchange.getResponseBody()) {
                                                outputStream.write(response);
                                        }
                                });

                OpenApiSpecFetcher fetcher = new OpenApiSpecFetcher(new OpenApiUrlValidator(true, true),
                                20 * 1024 * 1024, Duration.ofSeconds(30));

                String url = "http://localhost:"
                                + server.getAddress().getPort()
                                + "/large-openapi.yaml";

                IllegalStateException exception = org.junit.jupiter.api.Assertions
                                .assertThrows(
                                                IllegalStateException.class,
                                                () -> fetcher.fetch(url));

                org.junit.jupiter.api.Assertions.assertTrue(
                                exception.getMessage().contains("maximum allowed size"));
        }

        @Test
        void shouldRejectNotFoundResponse() throws IOException {

                server.createContext("/not-found", exchange -> {
                        exchange.sendResponseHeaders(404, -1);
                        exchange.close();
                });

                OpenApiSpecFetcher fetcher = new OpenApiSpecFetcher(new OpenApiUrlValidator(true, true),
                                20 * 1024 * 1024, Duration.ofSeconds(30));

                String url = "http://localhost:"
                                + server.getAddress().getPort()
                                + "/not-found";

                IllegalStateException exception = org.junit.jupiter.api.Assertions
                                .assertThrows(
                                                IllegalStateException.class,
                                                () -> fetcher.fetch(url));

                org.junit.jupiter.api.Assertions.assertTrue(
                                exception.getMessage().contains("HTTP status: 404"));
        }

        @Test
        void shouldRejectServerErrorResponse() throws IOException {

                server.createContext("/server-error", exchange -> {
                        exchange.sendResponseHeaders(500, -1);
                        exchange.close();
                });

                OpenApiSpecFetcher fetcher = new OpenApiSpecFetcher(new OpenApiUrlValidator(true, true),
                                20 * 1024 * 1024, Duration.ofSeconds(30));

                String url = "http://localhost:"
                                + server.getAddress().getPort()
                                + "/server-error";

                IllegalStateException exception = org.junit.jupiter.api.Assertions
                                .assertThrows(
                                                IllegalStateException.class,
                                                () -> fetcher.fetch(url));

                org.junit.jupiter.api.Assertions.assertTrue(
                                exception.getMessage().contains("HTTP status: 500"));
        }

}