package com.rateshield.service.impl;

import com.rateshield.dto.request.SimulateRateLimitRequest;
import com.rateshield.dto.response.SimulationResponse;
import com.rateshield.model.RateLimitPolicy;
import com.rateshield.model.RateLimitRequest;
import com.rateshield.ratelimiter.core.RateLimitResult;
import com.rateshield.ratelimiter.fixedwindow.FixedWindowRateLimiter;
import com.rateshield.ratelimiter.slidingwindow.SlidingWindowRateLimiter;
import com.rateshield.ratelimiter.tokenbucket.TokenBucketRateLimiter;
import com.rateshield.service.RateLimitingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RateLimitingServiceImpl implements RateLimitingService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingServiceImpl.class);

    private final FixedWindowRateLimiter fixedWindowRateLimiter;
    private final SlidingWindowRateLimiter slidingWindowRateLimiter;
    private final TokenBucketRateLimiter tokenBucketRateLimiter;

    public RateLimitingServiceImpl(
            FixedWindowRateLimiter fixedWindowRateLimiter,
            SlidingWindowRateLimiter slidingWindowRateLimiter,
            TokenBucketRateLimiter tokenBucketRateLimiter
    ) {
        this.fixedWindowRateLimiter = fixedWindowRateLimiter;
        this.slidingWindowRateLimiter = slidingWindowRateLimiter;
        this.tokenBucketRateLimiter = tokenBucketRateLimiter;
    }

    @Override
    public SimulationResponse simulate(SimulateRateLimitRequest request) {
        RateLimitPolicy policy = new RateLimitPolicy(
                "simulation-policy",
                "Simulation Policy",
                request.algorithm(),
                request.limit(),
                request.windowSizeInSeconds(),
                true
        );

        RateLimitRequest rateLimitRequest = new RateLimitRequest(
                request.subjectType(),
                request.subjectId(),
                request.resource()
        );

        RateLimitResult result = switch (request.algorithm()) {
            case FIXED_WINDOW -> fixedWindowRateLimiter.limit(rateLimitRequest, policy);
            case SLIDING_WINDOW -> slidingWindowRateLimiter.limit(rateLimitRequest, policy);
            case TOKEN_BUCKET -> tokenBucketRateLimiter.limit(rateLimitRequest, policy);
        };

        if (result.allowed()) {
            log.info(
                    "Simulation allowed. algorithm={}, subjectType={}, subjectId={}, resource={}, remaining={}, retryAfterSeconds={}",
                    request.algorithm(),
                    request.subjectType(),
                    request.subjectId(),
                    request.resource(),
                    result.remaining(),
                    result.retryAfterSeconds()
            );
        } else {
            log.warn(
                    "Simulation blocked. algorithm={}, subjectType={}, subjectId={}, resource={}, remaining={}, retryAfterSeconds={}",
                    request.algorithm(),
                    request.subjectType(),
                    request.subjectId(),
                    request.resource(),
                    result.remaining(),
                    result.retryAfterSeconds()
            );
        }

        return new SimulationResponse(
                result.allowed() ? "ALLOWED" : "BLOCKED",
                result.allowed(),
                result.remaining(),
                result.retryAfterSeconds(),
                result.message()
        );
    }
}
