package ca.uqac.scanner;

/**
 * The exit status of the scanner, as seen by the shell.
 */
enum ExitCode {

    /** The scan ran to its end, or was stopped on request. */
    SUCCESS(0),

    /** The target could not be resolved to an address. */
    UNRESOLVED_HOST(1),

    /** The command line was invalid, and the usage was printed. */
    USAGE_ERROR(2);

    private final int code;

    ExitCode(int code) {
        this.code = code;
    }

    /**
     * Returns the numeric status to pass to {@link System#exit(int)}.
     *
     * @return the status, from 0 to 2
     */
    int code() {
        return code;
    }
}
