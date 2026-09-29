package ca.uqac.scanner.core;

import java.net.InetAddress;
import java.util.Objects;

/**
 * A {@link PortScanner} that probes one port at a time, in ascending order.
 *
 * <p>It is simple and predictable, but slow on remote hosts: every filtered port costs a full timeout.
 * It ignores {@link ScanConfig#maxConcurrency()}. It serves as a reference implementation and suits the
 * cases where parallel connections are unwelcome.
 */
public final class SequentialPortScanner implements PortScanner {

    private final PortProbe probe;
    private final ScanConfig config;

    /**
     * Creates a sequential scanner.
     *
     * @param probe  tells whether each port is open
     * @param config the ports to scan and the probe timeout
     */
    public SequentialPortScanner(PortProbe probe, ScanConfig config) {
        this.probe = Objects.requireNonNull(probe, "probe");
        this.config = Objects.requireNonNull(config, "config");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Open ports are reported in ascending order, from the calling thread. Interruption is checked
     * between two probes, so a cancellation takes effect within one timeout.
     */
    @Override
    public void scan(InetAddress target, ScanListener listener) throws InterruptedException {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(listener, "listener");
        PortRange range = config.range();
        for (int port = range.first(); port <= range.last(); port++) {
            if (Thread.interrupted()) {
                throw new InterruptedException("Scan stopped");
            }
            if (probe.isOpen(target, port, config.timeout())) {
                listener.onOpenPort(port);
            }
        }
    }
}
