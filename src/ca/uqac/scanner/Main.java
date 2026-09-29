package ca.uqac.scanner;

import ca.uqac.scanner.core.ConcurrentPortScanner;
import ca.uqac.scanner.core.ScanConfig;
import ca.uqac.scanner.net.SystemHostResolver;
import ca.uqac.scanner.net.TcpPortProbe;

import java.time.Duration;

/**
 * Entry point of the TCP port scanner: wires the components together and registers the Ctrl+C handler.
 *
 * <p>Usage: {@code java -cp out ca.uqac.scanner.Main <target ip or hostname>}
 */
public final class Main {

    /** How long Ctrl+C waits for the probes in flight to stop before the JVM halts. */
    private static final Duration SHUTDOWN_GRACE_PERIOD = Duration.ofSeconds(2);

    private Main() {
    }

    /**
     * Scans the well-known TCP ports of a host, then exits with 0 on success, 1 if the host cannot be
     * resolved and 2 on invalid usage.
     *
     * @param args the IP address or host name to scan, as the single argument
     */
    public static void main(String[] args) {
        var session = new ScanSession(System.out);
        Runtime.getRuntime().addShutdownHook(new Thread(new ScanShutdownHook(session, SHUTDOWN_GRACE_PERIOD), "scan-shutdown"));
        var scanner = new ConcurrentPortScanner(new TcpPortProbe(), ScanConfig.DEFAULT);
        var cli = new ScannerCli(new SystemHostResolver(), scanner, session, System.err);
        System.exit(cli.run(args).code());
    }
}
