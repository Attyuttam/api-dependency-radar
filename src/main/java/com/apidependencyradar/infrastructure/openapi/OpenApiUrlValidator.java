package com.apidependencyradar.infrastructure.openapi;

import java.net.InetAddress;
import java.net.URI;

public class OpenApiUrlValidator {

    private final boolean allowHttp;
    private final boolean allowLocalAddresses;

    public OpenApiUrlValidator(boolean allowHttp) {
        this(allowHttp, false);
    }

    public OpenApiUrlValidator() {
        this(false, false);
    }

    public OpenApiUrlValidator(boolean allowHttp, boolean allowLocalAddresses) {
        this.allowHttp = allowHttp;
        this.allowLocalAddresses = allowLocalAddresses;
    }

    public void validate(String url) {

        final URI uri;

        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid URL", e);
        }

        String scheme = uri.getScheme();

        if (!allowHttp && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException(
                    "Only HTTPS URLs are allowed");
        }

        if (allowHttp
                && !"http".equalsIgnoreCase(scheme)
                && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException(
                    "Only HTTP and HTTPS URLs are allowed");
        }

        if (uri.getHost() == null) {
            throw new IllegalArgumentException(
                    "URL must contain a valid host");
        }

        try {
            InetAddress address = InetAddress.getByName(uri.getHost());

            if (!allowLocalAddresses
                    && (address.isAnyLocalAddress()
                            || address.isLoopbackAddress()
                            || address.isLinkLocalAddress()
                            || address.isSiteLocalAddress())) {

                throw new IllegalArgumentException(
                        "URL points to a private or local address");
            }

        } catch (java.net.UnknownHostException e) {
            throw new IllegalArgumentException(
                    "Unable to resolve host",
                    e);
        }
    }
}