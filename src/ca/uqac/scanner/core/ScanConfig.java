package ca.uqac.scanner.core;

import java.time.Duration;
import java.util.Objects;

/**
 * The settings of a port scan: which ports to probe, how long to wait for each of them and how many
 * probes may be in flight at the same time.
 *
 * @param range          the ports to scan
 * @param timeout        how long a probe waits for a connection before deeming the port not open;
 *                       must be positive
 * @param maxConcurrency the maximum number of probes in flight at the same time; must be at least 1
 */
public record ScanConfig(PortRange range, Duration timeout, int maxConcurrency) {

    /**
     * The settings of the command-line scanner: the {@linkplain PortRange#WELL_KNOWN well-known ports},
     * a one-second timeout and at most 200 probes in flight.
     *
     * <p>A host that refuses a connection answers within one round trip, so the timeout only matters
     * for filtered ports, and one second leaves a comfortable margin for remote targets. With 200 probes
     * in flight, even a host that filters every port is scanned in about six seconds, while staying far
     * below file descriptor limits and avoiding bursts of SYN packets that could be dropped along the
     * way, which would make open ports look closed.
     */
    public static final ScanConfig DEFAULT = new ScanConfig(PortRange.WELL_KNOWN, Duration.ofSeconds(1), 200);

    /**
     * Validates the settings.
     *
     * @throws NullPointerException     if {@code range} or {@code timeout} is {@code null}
     * @throws IllegalArgumentException if {@code timeout} is not positive or {@code maxConcurrency} is less than 1
     */
    public ScanConfig {
        Objects.requireNonNull(range, "range");
        Objects.requireNonNull(timeout, "timeout");
        if (!timeout.isPositive()) {
            throw new IllegalArgumentException("Timeout must be positive: " + timeout);
        }
        if (maxConcurrency < 1) {
            throw new IllegalArgumentException("Max concurrency must be at least 1: " + maxConcurrency);
        }
    }
}
