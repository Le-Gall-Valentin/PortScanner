package ca.uqac.scanner.net;

import ca.uqac.scanner.core.PortProbe;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;

/**
 * A {@link PortProbe} that establishes a full TCP connection with {@link Socket#connect(java.net.SocketAddress, int)}.
 *
 * <p>A port is open when the three-way handshake completes within the timeout; the connection is then
 * closed right away. Any {@link IOException}, whether the connection is refused, times out, cannot reach
 * the host or is interrupted, means that the port is not open. Unlike a SYN scan, this requires no
 * privileges, but the target may log the connection.
 */
public final class TcpPortProbe implements PortProbe {

    /**
     * Creates a TCP probe. It holds no state and can be shared between threads and scans.
     */
    public TcpPortProbe() {
    }

    /**
     * {@inheritDoc}
     *
     * <p>The timeout is truncated to whole milliseconds, with a minimum of one millisecond, since
     * {@code Socket#connect} would wait forever on zero.
     */
    @Override
    public boolean isOpen(InetAddress address, int port, Duration timeout) {
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress(address, port), Math.clamp(timeout.toMillis(), 1, Integer.MAX_VALUE));
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
