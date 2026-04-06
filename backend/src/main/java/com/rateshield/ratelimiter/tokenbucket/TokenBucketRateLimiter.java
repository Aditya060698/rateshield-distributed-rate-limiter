package com.rateshield.ratelimiter.tokenbucket;

import com.rateshield.model.RateLimitPolicy;
import com.rateshield.model.RateLimitRequest;
import com.rateshield.ratelimiter.core.RateLimitResult;
import com.rateshield.ratelimiter.core.RateLimiter;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * Token bucket stores the current token count and the last refill timestamp in Redis.
 *
 * Refill logic:
 * The bucket has a maximum capacity. Over time, tokens are added back at a steady rate.
 * In this simple version, the bucket refills from empty to full across the configured
 * window size. That means:
 * refill rate = capacity / window size
 *
 * Why token bucket is better:
 * Unlike fixed window, token bucket allows short bursts while still enforcing a stable
 * long-term rate. A client can use saved tokens for a burst, but sustained traffic still
 * gets throttled once the bucket is drained.
 *
 * Real-world usage:
 * Token bucket is common in API gateways, traffic shaping, and public APIs because it
 * balances user experience and protection better than a strict hard-window counter.
 *
 * Interview note:
 * Token bucket is often a stronger production choice than fixed window because it handles
 * burst traffic gracefully while remaining efficient in Redis.
 *
 * Interview note:
 * This version uses a Lua script so refill, consume, and state persistence happen as one
 * atomic Redis operation. That prevents multiple servers from spending the same token.
 */
@Component
public class TokenBucketRateLimiter implements RateLimiter {

    private static final String KEY_PREFIX = "rateshield";
    private static final long TTL_BUFFER_SECONDS = 1L;

    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<List> tokenBucketScript;
    private final Clock clock;

    public TokenBucketRateLimiter(
            StringRedisTemplate stringRedisTemplate,
            @Qualifier("tokenBucketScript") DefaultRedisScript<List> tokenBucketScript,
            Clock clock
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.tokenBucketScript = tokenBucketScript;
        this.clock = clock;
    }

    @Override
    public RateLimitResult limit(RateLimitRequest request, RateLimitPolicy policy) {
        Instant now = Instant.now(clock);
        long nowMillis = now.toEpochMilli();
        String key = buildKey(request);

        double capacity = policy.limit();
        double refillRatePerMilli = capacity / Duration.ofSeconds(policy.windowSizeInSeconds()).toMillis();
        long ttlSeconds = policy.windowSizeInSeconds() + TTL_BUFFER_SECONDS;

        List<?> result = stringRedisTemplate.execute(
                tokenBucketScript,
                List.of(key),
                String.valueOf(capacity),
                String.valueOf(refillRatePerMilli),
                String.valueOf(nowMillis),
                String.valueOf(ttlSeconds)
        );

        if (result == null || result.size() < 3) {
            throw new IllegalStateException("Redis did not return a valid token bucket result");
        }

        boolean allowed = toLong(result.get(0)) == 1L;
        int remaining = Math.toIntExact(toLong(result.get(1)));
        long retryAfterSeconds = toLong(result.get(2));

        if (!allowed) {
            return RateLimitResult.rejected(
                    retryAfterSeconds,
                    "Rate limit exceeded because the token bucket is empty"
            );
        }

        return RateLimitResult.allowed(
                remaining,
                "Request allowed using token bucket"
        );
    }

    private String buildKey(RateLimitRequest request) {
        return String.join(":",
                KEY_PREFIX,
                "token",
                normalize(request.subjectType()),
                normalize(request.subjectId()),
                normalize(request.resource())
        );
    }

    private String normalize(String value) {
        return value == null ? "unknown" : value.replace(":", "_").trim().toLowerCase();
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
