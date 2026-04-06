package com.rateshield.model;

public record RateLimitDecision(
        boolean allowed,
        int remaining,
        long retryAfterSeconds,
        String reason
) {
}
