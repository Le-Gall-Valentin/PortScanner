package ca.uqac.scanner;

import ca.uqac.scanner.core.PortScanner;
import ca.uqac.scanner.net.HostResolutionException;
import ca.uqac.scanner.net.HostResolver;

import java.io.PrintStream;
import java.net.InetAddress;
import java.util.Objects;

/**
 * The command-line scanner: checks the arguments, resolves the target and scans it.
 *
 * <p>The progress of the scan goes to the output of the {@link ScanSession}; the usage and the errors go
 * to the error stream.
 */
final class ScannerCli {

    /** The usage line, which shows the actual launch command. */
    static final String USAGE = "Usage: java -cp out ca.uqac.scanner.Main <target ip or hostname>";

    private final HostResolver resolver;
    private final PortScanner scanner;
    private final ScanSession session;
    private final PrintStream err;

    /**
     * Creates the command-line scanner.
     *
     * @param resolver resolves the target given on the command line
     * @param scanner  scans the resolved target
     * @param session  prints the progress of the scan, and lets the shutdown hook stop it
     * @param err      where the usage and the errors are printed
     */
    ScannerCli(HostResolver resolver, PortScanner scanner, ScanSession session, PrintStream err) {
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.scanner = Objects.requireNonNull(scanner, "scanner");
        this.session = Objects.requireNonNull(session, "session");
        this.err = Objects.requireNonNull(err, "err");
    }

    /**
     * Runs the scanner.
     *
     * @param args the command-line arguments: exactly one, the IP address or host name to scan
     * @return the exit status
     */
    ExitCode run(String... args) {
        // A blank target would silently resolve to the loopback address.
        if (args.length != 1 || args[0].isBlank()) {
            err.println(USAGE);
            return ExitCode.USAGE_ERROR;
        }
        String target = args[0];
        InetAddress address;
        try {
            address = resolver.resolve(target);
        } catch (HostResolutionException e) {
            err.println("Hostname could not be resolved: " + e.getMessage());
            return ExitCode.UNRESOLVED_HOST;
        }
        if (session.start(target)) {
            scan(address);
        }
        return ExitCode.SUCCESS;
    }

    private void scan(InetAddress address) {
        try {
            scanner.scan(address, session::reportOpenPort);
        } catch (InterruptedException e) {
            // The session interrupts this thread to stop the scan: the request is fully handled here.
        }
        session.complete();
    }
}
