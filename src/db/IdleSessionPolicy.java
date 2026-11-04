package db;

import java.util.concurrent.TimeUnit;

/**
 * Monotonic local workstation idle policy (15 minutes).
 * Not a server-side session or substitute for database authorization.
 */
public final class IdleSessionPolicy {
    private static final long TIMEOUT_NANOS = TimeUnit.MINUTES.toNanos(15);
    private IdleSessionPolicy() { }

    public static boolean isExpired(long lastActivity, long now) {
        return lastActivity > 0 && now - lastActivity >= TIMEOUT_NANOS;
    }
}
