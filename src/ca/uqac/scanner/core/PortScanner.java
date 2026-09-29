package ca.uqac.scanner.core;

import java.net.InetAddress;

/**
 * Scans the TCP ports of a host and reports the open ones.
 *
 * <p>The ports to scan, the probe timeout and the parallelism come from the {@link ScanConfig} an
 * implementation is built with. Open ports are reported to a {@link ScanListener} as soon as they are
 * found; closed ports are not reported.
 *
 * <p>A scan is cancelled by interrupting the thread that runs it: the scan then stops as soon as
 * possible and {@link #scan(InetAddress, ScanListener) scan} throws {@link InterruptedException}.
 */
public interface PortScanner {

    /**
     * Scans every port of the configured range on a host, blocking until all of them are probed.
     *
     * @param target   the host to scan
     * @param listener notified of each open port as soon as it is found
     * @throws InterruptedException if the calling thread is interrupted, which cancels the scan; the
     *                              ports reported until then are open, but the scan is incomplete
     */
    void scan(InetAddress target, ScanListener listener) throws InterruptedException;
}
