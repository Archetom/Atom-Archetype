package ${package}.shared.logging;

import java.io.Serial;

/**
 * Log-safe copy of a failure that keeps exception types and stack frames but drops messages.
 *
 * <p>Messages from drivers, clients, and parsers can contain credentials or personal data.
 * Log unexpected failures through this type so the logs still show where and why the
 * failure happened without copying arbitrary exception text.</p>
 */
public final class RedactedThrowable extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Guards against pathological or cyclic cause chains. */
    private static final int MAX_CAUSE_DEPTH = 16;

    private RedactedThrowable(Throwable original, int depth) {
        super(original.getClass().getName(), redactedCause(original, depth), false, true);
        setStackTrace(original.getStackTrace());
    }

    /** Returns a log-safe copy of the failure, or {@code null} when there is no failure. */
    public static RedactedThrowable of(Throwable original) {
        return original == null ? null : new RedactedThrowable(original, 0);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        // The original stack trace is copied in the constructor.
        return this;
    }

    private static RedactedThrowable redactedCause(Throwable original, int depth) {
        Throwable cause = original.getCause();
        if (cause == null || cause == original || depth >= MAX_CAUSE_DEPTH) {
            return null;
        }
        return new RedactedThrowable(cause, depth + 1);
    }
}
