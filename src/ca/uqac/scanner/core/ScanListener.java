package ca.uqac.scanner.core;

/**
 * Receives the open ports found by a {@link PortScanner}, as soon as they are found.
 *
 * <p>A concurrent scanner calls the listener from several threads at the same time and in no
 * particular port order: implementations must be thread-safe and must not expect ascending ports.
 */
@FunctionalInterface
public interface ScanListener {

    /**
     * Called once for each open port.
     *
     * @param port the port that accepted a connection
     */
    void onOpenPort(int port);
}
