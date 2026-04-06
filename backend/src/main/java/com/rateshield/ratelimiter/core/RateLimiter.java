package com.rateshield.ratelimiter.core;

import com.rateshield.model.RateLimitPolicy;
import com.rateshield.model.RateLimitRequest;

/**
 * This interface defines the contract that every rate limiting algorithm must follow.
 *
 * Interview note:
 * This is a classic Strategy Pattern. The main benefit is that the application depends
 * on an abstraction, not on a specific algorithm implementation.
 */
public interface RateLimiter {

    /**
     * Evaluates whether the incoming request is allowed under the given policy.
     *
     * Interview note:
     * A shared method signature makes it easy to switch from fixed window to sliding
     * window or token bucket without changing the calling service layer.
     */
    RateLimitResult limit(RateLimitRequest request, RateLimitPolicy policy);
}
