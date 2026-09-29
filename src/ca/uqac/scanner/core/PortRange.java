package ca.uqac.scanner.core;

import java.util.stream.IntStream;

/**
 * An inclusive range of TCP ports, such as {@code 1-1023}.
 *
 * <p>Port {@code 0} is outside every valid range: it is a wildcard asking the system for any free
 * port when binding a socket, so no service can listen on it and no connection can reach it.
 *
 * @param first the lowest port of the range, from {@value #MIN_PORT} to {@code last}
 * @param last  the highest port of the range, from {@code first} to {@value #MAX_PORT}
 */
public record PortRange(int first, int last) {

    /** The lowest port a connection can target. */
    public static final int MIN_PORT = 1;

    /** The highest TCP port. */
    public static final int MAX_PORT = 65_535;

    /**
     * The well-known ports, {@code 1-1023}, reserved by the IANA for system services (RFC 6335).
     */
    public static final PortRange WELL_KNOWN = new PortRange(MIN_PORT, 1023);

    /**
     * Validates the bounds of the range.
     *
     * @throws IllegalArgumentException if a bound is not a valid port or if {@code first > last}
     */
    public PortRange {
        if (first < MIN_PORT || last > MAX_PORT || first > last) {
            throw new IllegalArgumentException("Invalid port range %d-%d: expected %d <= first <= last <= %d"
                    .formatted(first, last, MIN_PORT, MAX_PORT));
        }
    }

    /**
     * Returns the ports of this range in ascending order.
     *
     * @return the ports from {@link #first()} to {@link #last()}, both included
     */
    public IntStream ports() {
        return IntStream.rangeClosed(first, last);
    }
}
