package com.apidependencyradar.infrastructure.openapi;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

public class OpenApiUrlValidator {

    private final boolean allowHttp;
    private final boolean allowLocalAddresses;

    public OpenApiUrlValidator(boolean allowHttp) {
        this(allowHttp, false);
    }

    public OpenApiUrlValidator() {
        this(false, false);
    }

    public OpenApiUrlValidator(
            boolean allowHttp,
            boolean allowLocalAddresses) {
        this.allowHttp = allowHttp;
        this.allowLocalAddresses = allowLocalAddresses;
    }

    public void validate(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL must not be blank");
        }

        final URI uri;

        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid URL", e);
        }

        String scheme = uri.getScheme();

        boolean isHttps = "https".equalsIgnoreCase(scheme);
        boolean isHttp = "http".equalsIgnoreCase(scheme);

        if (!isHttps && !(allowHttp && isHttp)) {
            throw new IllegalArgumentException(
                    allowHttp
                            ? "Only HTTP and HTTPS URLs are allowed"
                            : "Only HTTPS URLs are allowed");
        }

        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException(
                    "URL must contain a valid host");
        }

        if (uri.getRawUserInfo() != null) {
            throw new IllegalArgumentException(
                    "URLs containing user information are not allowed");
        }

        int port = uri.getPort();

        if (port != -1 && (port < 1 || port > 65535)) {
            throw new IllegalArgumentException("Invalid URL port");
        }

        if (uri.getRawAuthority() == null
                || uri.getRawAuthority().isBlank()) {
            throw new IllegalArgumentException(
                    "URL must contain a valid host");
        }

        if (allowLocalAddresses) {
            return;
        }

        String host = uri.getHost();

        // URI host values for IPv6 literals can include square brackets.
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }

        final InetAddress[] addresses;

        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(
                    "Unable to resolve host", e);
        }

        if (addresses.length == 0) {
            throw new IllegalArgumentException(
                    "Unable to resolve host");
        }

        validateResolvedAddresses(addresses);
    }

    void validateResolvedAddresses(InetAddress[] addresses) {
        if (allowLocalAddresses) {
            return;
        }

        if (addresses == null || addresses.length == 0) {
            throw new IllegalArgumentException("Unable to resolve host");
        }

        // Reject the entire hostname if any resolved address is unsafe.
        for (InetAddress address : addresses) {
            if (!isAllowedPublicAddress(address)) {
                throw new IllegalArgumentException(
                        "URL points to a private, local, or special-use address");
            }
        }
    }

    private boolean isAllowedPublicAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return false;
        }

        byte[] bytes = address.getAddress();

        if (address instanceof Inet4Address) {
            return isPublicIpv4(bytes);
        }

        if (address instanceof Inet6Address) {
            return isPublicIpv6(bytes);
        }

        return false;
    }

    private boolean isPublicIpv4(byte[] ip) {
        int a = ip[0] & 0xff;
        int b = ip[1] & 0xff;
        int c = ip[2] & 0xff;

        // Unspecified/current network and private ranges.
        if (a == 0
                || a == 10
                || a == 127
                || (a == 172 && b >= 16 && b <= 31)
                || (a == 192 && b == 168)) {
            return false;
        }

        // Carrier-grade NAT: 100.64.0.0/10.
        if (a == 100 && b >= 64 && b <= 127) {
            return false;
        }

        // Link-local.
        if (a == 169 && b == 254) {
            return false;
        }

        // IETF special-purpose assignments.
        if (a == 192 && b == 0 && c == 0) {
            return false;
        }

        // Documentation ranges.
        if ((a == 192 && b == 0 && c == 2)
                || (a == 198 && b == 51 && c == 100)
                || (a == 203 && b == 0 && c == 113)) {
            return false;
        }

        // Deprecated 6to4 relay anycast range.
        if (a == 192 && b == 88 && c == 99) {
            return false;
        }

        // Benchmarking.
        if (a == 198 && (b == 18 || b == 19)) {
            return false;
        }

        // Multicast and reserved high addresses.
        if (a >= 224) {
            return false;
        }

        return true;
    }

    private boolean isPublicIpv6(byte[] ip) {
        // Allow only global-unicast space: 2000::/3.
        // Reject other address categories by default.
        if ((ip[0] & 0xe0) != 0x20) {
            return false;
        }

        int first = ip[0] & 0xff;
        int second = ip[1] & 0xff;
        int third = ip[2] & 0xff;
        int fourth = ip[3] & 0xff;
        int fifth = ip[4] & 0xff;
        int sixth = ip[5] & 0xff;

        // Documentation prefix: 2001:db8::/32.
        if (first == 0x20
                && second == 0x01
                && third == 0x0d
                && fourth == 0xb8) {
            return false;
        }

        // Benchmarking prefix: 2001:2::/48.
        if (first == 0x20
                && second == 0x01
                && third == 0x00
                && fourth == 0x02
                && fifth == 0x00
                && sixth == 0x00) {
            return false;
        }
        // Teredo transition prefix: 2001::/32.
        if (first == 0x20
                && second == 0x01
                && third == 0x00
                && fourth == 0x00) {
            return false;
        }

        // Deprecated ORCHID prefix: 2001:10::/28.
        if (first == 0x20
                && second == 0x01
                && third == 0x00
                && (fourth & 0xf0) == 0x10) {
            return false;
        }

        // ORCHIDv2 prefix: 2001:20::/28.
        if (first == 0x20
                && second == 0x01
                && third == 0x00
                && (fourth & 0xf0) == 0x20) {
            return false;
        }

        // 6to4 transition prefix: 2002::/16.
        if (first == 0x20 && second == 0x02) {
            return false;
        }

        // Documentation prefix: 3fff::/20.
        if (first == 0x3f
                && second == 0xff
                && (third & 0xf0) == 0x00) {
            return false;
        }

        return true;
    }
}