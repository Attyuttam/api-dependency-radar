package com.apidependencyradar.infrastructure.openapi;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.util.Timeout;
import java.time.Duration;

public class OpenApiSpecFetcher implements AutoCloseable {

    private static final int MAX_RESPONSE_SIZE_BYTES = 5 * 1024 * 1024;
    private static final Duration MAX_FETCH_DURATION = Duration.ofSeconds(30);

    private final CloseableHttpClient httpClient;
    private final OpenApiUrlValidator urlValidator;

    public OpenApiSpecFetcher() {
        this(new OpenApiUrlValidator());
    }

    public OpenApiSpecFetcher(OpenApiUrlValidator urlValidator) {
        this.urlValidator = urlValidator;

        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(5))
                .build();

        var connectionManager = PoolingHttpClientConnectionManagerBuilder
                .create()
                .setDnsResolver(new ValidatingDnsResolver(urlValidator))
                .setDefaultConnectionConfig(connectionConfig)
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setResponseTimeout(Timeout.ofSeconds(10))
                .build();

        this.httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .disableRedirectHandling()
                .build();
    }

    public String fetch(String url) {
        long deadlineNanos = System.nanoTime() + MAX_FETCH_DURATION.toNanos();

        urlValidator.validate(url);
        ensureBeforeDeadline(deadlineNanos);

        try {
            HttpGet request = new HttpGet(URI.create(url));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                ensureBeforeDeadline(deadlineNanos);
                int statusCode = response.getCode();

                if (statusCode < 200 || statusCode >= 300) {
                    throw new IllegalStateException(
                            "Failed to fetch OpenAPI specification. HTTP status: "
                                    + statusCode);
                }

                HttpEntity entity = response.getEntity();

                if (entity == null) {
                    throw new IllegalStateException(
                            "OpenAPI specification response has no body");
                }

                try (InputStream inputStream = entity.getContent();
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                    byte[] buffer = new byte[8192];
                    int totalBytes = 0;
                    int bytesRead;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        ensureBeforeDeadline(deadlineNanos);

                        totalBytes += bytesRead;

                        if (totalBytes > MAX_RESPONSE_SIZE_BYTES) {
                            throw new IllegalStateException(
                                    "OpenAPI specification exceeds maximum allowed size");
                        }

                        outputStream.write(buffer, 0, bytesRead);
                    }

                    return outputStream.toString(StandardCharsets.UTF_8);
                }
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to fetch OpenAPI specification", e);
        }
    }

    @Override
    public void close() throws Exception {
        httpClient.close();
    }

    private void ensureBeforeDeadline(long deadlineNanos) {
        if (System.nanoTime() - deadlineNanos >= 0) {
            throw new IllegalStateException(
                    "OpenAPI fetch exceeded maximum allowed duration");
        }
    }
}