package com.rateshield.ratelimiter.slidingwindow;

import com.rateshield.model.RateLimitPolicy;
import com.rateshield.model.RateLimitRequest;
import com.rateshield.ratelimiter.core.RateLimitResult;
import com.rateshield.ratelimiter.core.RateLimiter;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * Sliding window rate limiting keeps individual request timestamps in a Redis sorted set.
 *
 * Why a sorted set is used:
 * A sorted set stores members ordered by score. Here the score is the request timestamp,
 * so Redis can efficiently remove old requests and count only the requests that still
 * belong to the active rolling window.
 *
 * How timestamps work:
 * Each request is stored with the current epoch time in milliseconds as its score.
 * When a new request arrives, entries older than "now - window size" are removed.
 * The remaining entries represent requests made in the last N seconds.
 *
 * Trade-offs:
 * Sliding window is more accurate and fair than fixed window because it avoids hard
 * bucket boundaries. The cost is higher Redis usage: more memory, more commands, and
 * more work per request.
 *
 * Learning note:
 * This algorithm is easier to reason about when you think of it as "keep only the last
 * valid timestamps, then count them."
 *
 * Learning note:
 * Cleanup, count, add, and expire must behave like one unit. The Lua script keeps the
 * whole decision atomic so multiple servers cannot interleave these steps.
 */
@Component
public class SlidingWindowRateLimiter implements RateLimiter {

    private static final String KEY_PREFIX = "rateshield";
    private static final long TTL_BUFFER_SECONDS = 1L;

    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<List> slidingWindowScript;
    private final Clock clock;

    public SlidingWindowRateLimiter(
            StringRedisTemplate stringRedisTemplate,
            @Qualifier("slidingWindowScript") DefaultRedisScript<List> slidingWindowScript,
            Clock clock
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.slidingWindowScript = slidingWindowScript;
        this.clock = clock;
    }

    @Override
    public RateLimitResult limit(RateLimitRequest request, RateLimitPolicy policy) {
        Instant now = Instant.now(clock);
        long nowMillis = now.toEpochMilli();
        long windowSizeInMillis = Duration.ofSeconds(policy.windowSizeInSeconds()).toMillis();
        long windowStartMillis = nowMillis - windowSizeInMillis;
        long retryAfterSeconds = Math.max(policy.windowSizeInSeconds(), 0);

        String key = buildKey(request);
        String member = nowMillis + "-" + UUID.randomUUID();
        long ttlSeconds = policy.windowSizeInSeconds() + TTL_BUFFER_SECONDS;

        List<?> result = stringRedisTemplate.execute(
                slidingWindowScript,
                List.of(key),
                String.valueOf(windowStartMillis),
                String.valueOf(policy.limit()),
                String.valueOf(nowMillis),
                member,
                String.valueOf(ttlSeconds)
        );

        if (result == null || result.size() < 2) {
            throw new IllegalStateException("Redis did not return a valid sliding window result");
        }

        boolean allowed = toLong(result.get(0)) == 1L;
        int remaining = Math.toIntExact(toLong(result.get(1)));

        if (!allowed) {
            return RateLimitResult.rejected(
                    retryAfterSeconds,
                    "Rate limit exceeded for the current sliding window"
            );
        }

        return RateLimitResult.allowed(remaining, "Request allowed in current sliding window");
    }

    private String buildKey(RateLimitRequest request) {
        return String.join(":",
                KEY_PREFIX,
                "sliding",
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
