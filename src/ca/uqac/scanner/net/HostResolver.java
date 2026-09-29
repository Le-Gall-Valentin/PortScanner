package ca.uqac.scanner.net;

import java.net.InetAddress;

/**
 * Turns a host name or an IP address literal into an address to connect to.
 */
@FunctionalInterface
public interface HostResolver {

    /**
     * Resolves a host.
     *
     * @param host a host name such as {@code example.com}, or an IPv4 or IPv6 address literal
     * @return the address of the host
     * @throws HostResolutionException if the host cannot be resolved
     */
    InetAddress resolve(String host) throws HostResolutionException;
}
