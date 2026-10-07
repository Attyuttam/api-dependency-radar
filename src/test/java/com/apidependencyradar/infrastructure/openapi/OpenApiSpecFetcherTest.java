package com.apidependencyradar.infrastructure.openapi;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;

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

        OpenApiSpecFetcher fetcher = new OpenApiSpecFetcher(new OpenApiUrlValidator(true, true));
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
}