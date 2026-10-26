package com.apidependencyradar.infrastructure.openapi;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.hc.client5.http.DnsResolver;
import org.junit.jupiter.api.Test;

class ValidatingDnsResolverTest {

    @Test
    void acceptsPublicAddress() throws Exception {
        InetAddress publicAddress = InetAddress.getByAddress(new byte[] {
                (byte) 1, (byte) 1, (byte) 1, (byte) 1
        });

        ValidatingDnsResolver resolver = resolverReturning(publicAddress);

        assertArrayEquals(
                new InetAddress[] { publicAddress },
                resolver.resolve("public.example"));
    }

    @Test
    void rejectsLoopbackAddress() throws Exception {
        InetAddress loopbackAddress = InetAddress.getByAddress(new byte[] {
                127, 0, 0, 1
        });

        ValidatingDnsResolver resolver = resolverReturning(loopbackAddress);

        assertThrows(
                UnknownHostException.class,
                () -> resolver.resolve("internal.example"));
    }

    @Test
    void rejectsMixedPublicAndPrivateAddresses() throws Exception {
        InetAddress publicAddress = InetAddress.getByAddress(new byte[] {
                (byte) 1, (byte) 1, (byte) 1, (byte) 1
        });

        InetAddress privateAddress = InetAddress.getByAddress(new byte[] {
                (byte) 192, (byte) 168, 1, 10
        });

        ValidatingDnsResolver resolver = resolverReturning(publicAddress, privateAddress);

        assertThrows(
                UnknownHostException.class,
                () -> resolver.resolve("mixed.example"));
    }

    private ValidatingDnsResolver resolverReturning(
            InetAddress... addresses) {

        DnsResolver fakeResolver = new DnsResolver() {
            @Override
            public InetAddress[] resolve(String host)
                    throws UnknownHostException {
                return addresses;
            }

            @Override
            public String resolveCanonicalHostname(String host)
                    throws UnknownHostException {
                return host;
            }
        };

        return new ValidatingDnsResolver(
                new OpenApiUrlValidator(),
                fakeResolver);
    }
}