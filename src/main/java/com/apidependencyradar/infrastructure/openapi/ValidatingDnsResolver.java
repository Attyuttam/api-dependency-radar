package com.apidependencyradar.infrastructure.openapi;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.SystemDefaultDnsResolver;

public final class ValidatingDnsResolver implements DnsResolver {

    private final OpenApiUrlValidator urlValidator;
    private final DnsResolver delegate;

    public ValidatingDnsResolver(OpenApiUrlValidator urlValidator) {
        this(urlValidator, SystemDefaultDnsResolver.INSTANCE);
    }

    ValidatingDnsResolver(
            OpenApiUrlValidator urlValidator,
            DnsResolver delegate) {

        if (urlValidator == null) {
            throw new IllegalArgumentException(
                    "URL validator must not be null");
        }

        if (delegate == null) {
            throw new IllegalArgumentException(
                    "DNS resolver must not be null");
        }

        this.urlValidator = urlValidator;
        this.delegate = delegate;
    }

    @Override
    public InetAddress[] resolve(String host) throws UnknownHostException {
        InetAddress[] addresses = delegate.resolve(host);

        try {
            urlValidator.validateResolvedAddresses(addresses);
        } catch (IllegalArgumentException e) {
            UnknownHostException exception = new UnknownHostException(
                    "Host resolves to a private, local, or special-use address");
            exception.initCause(e);
            throw exception;
        }

        return addresses;
    }

    @Override
    public String resolveCanonicalHostname(String host)
            throws UnknownHostException {
        return delegate.resolveCanonicalHostname(host);
    }
}