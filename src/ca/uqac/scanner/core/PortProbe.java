package ca.uqac.scanner.core;

import java.net.InetAddress;
import java.time.Duration;

/**
 * Tells whether a TCP port of a host accepts connections.
 *
 * <p>Implementations must be thread-safe, since a scanner may probe many ports at the same time.
 */
@FunctionalInterface
public interface PortProbe {

    /**
     * Probes a port of a host.
     *
     * <p>Any failure, such as a refused connection, a timeout, an unreachable host or an interruption,
     * means that the port is not open: implementations report it by returning {@code false}, never by
     * throwing an exception.
     *
     * @param address the host to probe
     * @param port    the port to probe, from {@value PortRange#MIN_PORT} to {@value PortRange#MAX_PORT}
     * @param timeout how long to wait for the host to answer before giving up
     * @return {@code true} if the port accepted a connection within the timeout
     */
    boolean isOpen(InetAddress address, int port, Duration timeout);
}
