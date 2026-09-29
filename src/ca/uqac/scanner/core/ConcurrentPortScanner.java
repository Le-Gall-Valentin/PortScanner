package ca.uqac.scanner.core;

import java.net.InetAddress;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * A {@link PortScanner} that probes many ports at the same time, each one on its own virtual thread.
 *
 * <p>One virtual thread is started per port, and a {@link Semaphore} caps the number of probes in flight
 * at {@link ScanConfig#maxConcurrency()}. This cap is a safeguard, not a limitation: opening every
 * connection at once could exhaust file descriptors or overflow the queues of the network stack, and a
 * dropped SYN packet would make an open port look closed.
 *
 * <p>The listener is called from the probing threads, concurrently, as soon as each open port is found:
 * <strong>the reporting order is not guaranteed</strong>. The semaphore is fair, so probes start in
 * roughly ascending port order, but a port that answers faster can be reported before a lower one.
 *
 * <p>Interrupting the scanning thread cancels the scan: pending probes are skipped, and connections in
 * progress are aborted at once, since the socket operations of virtual threads are interruptible.
 */
public final class ConcurrentPortScanner implements PortScanner {

    private final PortProbe probe;
    private final ScanConfig config;

    /**
     * Creates a concurrent scanner.
     *
     * @param probe  tells whether each port is open; called from many threads at the same time
     * @param config the ports to scan, the probe timeout and the maximum number of probes in flight
     */
    public ConcurrentPortScanner(PortProbe probe, ScanConfig config) {
        this.probe = Objects.requireNonNull(probe, "probe");
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public void scan(InetAddress target, ScanListener listener) throws InterruptedException {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(listener, "listener");
        var permits = new Semaphore(config.maxConcurrency(), true);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            config.range().ports().forEach(port -> executor.execute(() -> scanPort(target, port, permits, listener)));
        } // close() waits for every probe; if interrupted, it aborts them all and restores the interrupt status
        if (Thread.interrupted()) {
            throw new InterruptedException("Scan stopped");
        }
    }

    private void scanPort(InetAddress target, int port, Semaphore permits, ScanListener listener) {
        try {
            permits.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return; // the scan was cancelled before this port's turn
        }
        try {
            if (probe.isOpen(target, port, config.timeout())) {
                listener.onOpenPort(port);
            }
        } finally {
            permits.release();
        }
    }
}
