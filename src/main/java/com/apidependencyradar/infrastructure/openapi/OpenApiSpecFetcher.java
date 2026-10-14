package com.apidependencyradar.infrastructure.openapi;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class OpenApiSpecFetcher {

    private final HttpClient httpClient;
    private final OpenApiUrlValidator urlValidator;

    private static final int MAX_RESPONSE_SIZE_BYTES = 5 * 1024 * 1024;

    public OpenApiSpecFetcher() {
        this(new OpenApiUrlValidator());
    }

    public OpenApiSpecFetcher(OpenApiUrlValidator urlValidator) {

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(5))
                .build();
        this.urlValidator = urlValidator;

    }

    public String fetch(String url) {
        urlValidator.validate(url);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<InputStream> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                try (InputStream ignored = response.body()) {
                    // Close the response body.
                }

                throw new IllegalStateException(
                        "Failed to fetch OpenAPI specification. HTTP status: "
                                + response.statusCode());
            }

            try (InputStream inputStream = response.body();
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                byte[] buffer = new byte[8192];
                int totalBytes = 0;
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {

                    totalBytes += bytesRead;

                    if (totalBytes > MAX_RESPONSE_SIZE_BYTES) {
                        throw new IllegalStateException(
                                "OpenAPI specification exceeds maximum allowed size");
                    }

                    outputStream.write(buffer, 0, bytesRead);
                }

                return outputStream.toString(StandardCharsets.UTF_8);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "OpenAPI fetch was interrupted",
                    e);

        } catch (IllegalStateException e) {
            throw e;

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to fetch OpenAPI specification",
                    e);
        }
    }

}