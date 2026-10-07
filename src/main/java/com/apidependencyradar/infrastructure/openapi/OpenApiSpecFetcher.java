package com.apidependencyradar.infrastructure.openapi;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class OpenApiSpecFetcher {

    private final HttpClient httpClient;
    private final OpenApiUrlValidator urlValidator;

    public OpenApiSpecFetcher() {
        this(new OpenApiUrlValidator());
    }

    OpenApiSpecFetcher(OpenApiUrlValidator urlValidator) {

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

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Failed to fetch OpenAPI specification. HTTP status: "
                                + response.statusCode());
            }

            return response.body();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to fetch OpenAPI specification",
                    e);
        }
    }
}