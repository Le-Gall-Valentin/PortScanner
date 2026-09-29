package ca.uqac.scanner.net;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * A {@link HostResolver} backed by the resolver of the operating system, through
 * {@link InetAddress#getByName(String)}: IP address literals are parsed without any lookup, and host
 * names go through the hosts file and DNS.
 *
 * <p>When a name maps to several addresses, the first one is returned, following the system preference
 * between IPv4 and IPv6 (see the {@code java.net.preferIPv6Addresses} property).
 */
public final class SystemHostResolver implements HostResolver {

    /**
     * Creates a resolver backed by the operating system.
     */
    public SystemHostResolver() {
    }

    @Override
    public InetAddress resolve(String host) throws HostResolutionException {
        try {
            return InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new HostResolutionException(host, e);
        }
    }
}
