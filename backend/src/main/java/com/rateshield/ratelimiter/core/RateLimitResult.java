package com.rateshield.ratelimiter.core;

/**
 * A dedicated result object keeps the algorithm output independent from transport
 * concerns such as controller responses or HTTP status mapping.
 *
 * Interview note:
 * Returning a common result model from all algorithms keeps the service layer stable
 * and makes algorithm implementations easy to compare, test, and replace.
 */
public record RateLimitResult(
        boolean allowed,
        int remaining,
        long retryAfterSeconds,
        String message
) {

    public static RateLimitResult allowed(int remaining, String message) {
        return new RateLimitResult(true, remaining, 0, message);
    }

    public static RateLimitResult rejected(long retryAfterSeconds, String message) {
        return new RateLimitResult(false, 0, retryAfterSeconds, message);
    }
}
