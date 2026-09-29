package ca.uqac.scanner.net;

import java.io.Serial;
import java.util.Objects;

/**
 * Thrown when a host cannot be resolved to an address.
 *
 * <p>Its message is the one of the underlying failure, whose wording depends on the platform, for
 * instance {@code nope.invalid: Name or service not known} on Linux.
 */
public final class HostResolutionException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String host;

    /**
     * Creates an exception for a host that could not be resolved.
     *
     * @param host  the host that could not be resolved
     * @param cause the resolution failure, whose message becomes the message of this exception
     */
    public HostResolutionException(String host, Throwable cause) {
        super(Objects.requireNonNullElse(cause.getMessage(), host), cause);
        this.host = host;
    }

    /**
     * Returns the host that could not be resolved.
     *
     * @return the host, as given to the resolver
     */
    public String host() {
        return host;
    }
}
