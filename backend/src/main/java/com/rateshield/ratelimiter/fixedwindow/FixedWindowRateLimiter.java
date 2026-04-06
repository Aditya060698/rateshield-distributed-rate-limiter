package com.rateshield.ratelimiter.fixedwindow;

import com.rateshield.model.RateLimitPolicy;
import com.rateshield.model.RateLimitRequest;
import com.rateshield.ratelimiter.core.RateLimitResult;
import com.rateshield.ratelimiter.core.RateLimiter;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * Fixed window rate limiting stores one Redis counter per subject and per time bucket.
 *
 * Example:
 * A 60-second policy uses a different counter for each minute window.
 *
 * Burst problem:
 * Fixed window is simple and fast, but it allows burst traffic near window boundaries.
 * A client can send requests at the end of one window and again at the start of the next
 * window, which can produce a short spike that is larger than the configured limit.
 *
 * Limitations:
 * This implementation is intentionally simple. It is a good starting point, but it is
 * less fair than sliding window and less flexible than token bucket.
 *
 * Interview note:
 * This algorithm is often chosen first because it proves the end-to-end Redis-based
 * design with minimal operational complexity.
 *
 * Concurrency note:
 * Even fixed window benefits from atomicity. The Lua script makes the increment and TTL
 * setup happen as one Redis operation, which avoids partial updates under concurrency.
 */
@Component
public class FixedWindowRateLimiter implements RateLimiter {

    private static final String KEY_PREFIX = "rateshield";
    private static final long TTL_BUFFER_SECONDS = 1L;

    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<List> fixedWindowScript;
    private final Clock clock;

    public FixedWindowRateLimiter(
            StringRedisTemplate stringRedisTemplate,
            @Qualifier("fixedWindowScript") DefaultRedisScript<List> fixedWindowScript,
            Clock clock
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.fixedWindowScript = fixedWindowScript;
        this.clock = clock;
    }

    @Override
    public RateLimitResult limit(RateLimitRequest request, RateLimitPolicy policy) {
        Instant now = Instant.now(clock);
        long currentEpochSecond = now.getEpochSecond();
        long windowSizeInSeconds = policy.windowSizeInSeconds();
        long windowBucket = currentEpochSecond / windowSizeInSeconds;
        long windowEndEpochSecond = (windowBucket + 1) * windowSizeInSeconds;
        long retryAfterSeconds = Math.max(windowEndEpochSecond - currentEpochSecond, 0);

        String key = buildKey(request, windowBucket);
        long ttlSeconds = retryAfterSeconds + TTL_BUFFER_SECONDS;

        List<?> result = stringRedisTemplate.execute(
                fixedWindowScript,
                List.of(key),
                String.valueOf(ttlSeconds),
                String.valueOf(policy.limit())
        );

        if (result == null || result.size() < 2) {
            throw new IllegalStateException("Redis did not return a valid fixed window result");
        }

        boolean allowed = toLong(result.get(0)) == 1L;
        int remaining = Math.toIntExact(toLong(result.get(1)));

        if (!allowed) {
            return RateLimitResult.rejected(
                    retryAfterSeconds,
                    "Rate limit exceeded for the current fixed window"
            );
        }

        return RateLimitResult.allowed(remaining, "Request allowed in current fixed window");
    }

    private String buildKey(RateLimitRequest request, long windowBucket) {
        return String.join(":",
                KEY_PREFIX,
                "fixed",
                normalize(request.subjectType()),
                normalize(request.subjectId()),
                normalize(request.resource()),
                String.valueOf(windowBucket)
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
