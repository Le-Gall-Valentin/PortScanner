package ca.uqac.scanner;

import java.time.Duration;
import java.util.Objects;

/**
 * Stops a running scan gracefully when the JVM shuts down, typically on Ctrl+C.
 *
 * <p>As a shutdown hook, it also runs on every normal exit: the scan is then over, or never started, and
 * the hook prints nothing. Otherwise it stops the scan, waits a bounded time for the probes to wind down,
 * since the JVM halts as soon as the hook returns, and makes sure the final message is printed.
 */
final class ScanShutdownHook implements Runnable {

    private final ScanSession session;
    private final Duration gracePeriod;

    /**
     * Creates the hook of a session.
     *
     * @param session     the session to stop
     * @param gracePeriod how long to wait for the scan to stop before printing the final message anyway
     */
    ScanShutdownHook(ScanSession session, Duration gracePeriod) {
        this.session = Objects.requireNonNull(session, "session");
        this.gracePeriod = Objects.requireNonNull(gracePeriod, "gracePeriod");
    }

    @Override
    public void run() {
        if (!session.stop()) {
            return; // normal exit: no scan in progress
        }
        try {
            session.awaitTermination(gracePeriod);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        session.complete(); // no-op if the scanning thread already completed the session
    }
}
