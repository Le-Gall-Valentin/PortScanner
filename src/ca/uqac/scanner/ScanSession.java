package ca.uqac.scanner;

import java.io.PrintStream;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The console side of a scan: prints its progress and tracks its lifecycle, so that the main thread,
 * the probing threads and the shutdown hook can report concurrently while the output stays consistent.
 *
 * <p>A session goes through {@code IDLE -> RUNNING -> STOPPING -> TERMINATED}. A scan that runs to its
 * end skips {@code STOPPING}, and a session stopped before it started goes straight from {@code IDLE} to
 * {@code TERMINATED}, silently. Every transition and every line happens under a single lock, which
 * guarantees, whatever the interleaving of the threads, that:
 * <ul>
 *   <li>{@code Scanning <target>...} is printed first, and once;</li>
 *   <li>open ports are only printed while the scan runs, hence never after {@code Scan is stopping...};</li>
 *   <li>{@code Scan is stopping...} and {@code Scan completed successfully.} are each printed at most once.</li>
 * </ul>
 *
 * <p>A compare-and-set on the state alone would not be enough: checking the state and printing must be
 * atomic together. A {@link ReentrantLock} is used rather than {@code synchronized} so that a virtual
 * thread printing an open port does not pin its carrier thread.
 */
final class ScanSession {

    private enum State { IDLE, RUNNING, STOPPING, TERMINATED }

    private final PrintStream out;
    private final Lock lock = new ReentrantLock();
    private final CountDownLatch terminated = new CountDownLatch(1);
    private State state = State.IDLE;
    private Thread scanThread;

    /**
     * Creates an idle session.
     *
     * @param out where the progress of the scan is printed
     */
    ScanSession(PrintStream out) {
        this.out = Objects.requireNonNull(out, "out");
    }

    /**
     * Starts the session: prints {@code Scanning <target>...} and binds the session to the calling
     * thread, which {@link #stop()} interrupts to cancel the scan.
     *
     * @param target the host being scanned, as given by the user
     * @return {@code true} if the session started; {@code false} if it was already started or stopped,
     *         in which case nothing is printed and no scan should run
     */
    boolean start(String target) {
        lock.lock();
        try {
            if (state != State.IDLE) {
                return false;
            }
            out.println("Scanning " + target + "...");
            scanThread = Thread.currentThread();
            state = State.RUNNING;
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Prints an open port, unless the scan is no longer running. Can be called from any thread.
     *
     * @param port the port found open
     */
    void reportOpenPort(int port) {
        lock.lock();
        try {
            if (state == State.RUNNING) {
                out.println("--> Port " + port + "/TCP is open.");
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Stops the session. A running scan prints {@code Scan is stopping...} and its thread is interrupted;
     * a session that has not started yet is terminated silently, so that it never starts.
     *
     * @return {@code true} if this call stopped a running scan
     */
    boolean stop() {
        lock.lock();
        try {
            return switch (state) {
                case IDLE -> {
                    terminate();
                    yield false;
                }
                case RUNNING -> {
                    out.println("Scan is stopping...");
                    state = State.STOPPING;
                    scanThread.interrupt();
                    yield true;
                }
                case STOPPING, TERMINATED -> false;
            };
        } finally {
            lock.unlock();
        }
    }

    /**
     * Prints {@code Scan completed successfully.} and terminates the session, if it was started and not
     * yet terminated; otherwise does nothing. The scanning thread calls it when the scan returns, and the
     * shutdown hook calls it again in case the scan did not stop in time: only the first call prints.
     */
    void complete() {
        lock.lock();
        try {
            if (state == State.RUNNING || state == State.STOPPING) {
                out.println("Scan completed successfully.");
                terminate();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Waits for the session to terminate, for at most the given duration.
     *
     * @param timeout the maximum time to wait
     * @return {@code true} if the session terminated, {@code false} if the timeout elapsed first
     * @throws InterruptedException if the calling thread is interrupted while waiting
     */
    boolean awaitTermination(Duration timeout) throws InterruptedException {
        return terminated.await(TimeUnit.NANOSECONDS.convert(timeout), TimeUnit.NANOSECONDS);
    }

    private void terminate() {
        state = State.TERMINATED;
        terminated.countDown();
    }
}
