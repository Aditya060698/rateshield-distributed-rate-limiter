package com.rateshield.ratelimiter.slidingwindow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rateshield.model.AlgorithmType;
import com.rateshield.model.RateLimitPolicy;
import com.rateshield.model.RateLimitRequest;
import com.rateshield.ratelimiter.core.RateLimitResult;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@ExtendWith(MockitoExtension.class)
class SlidingWindowRateLimiterTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private DefaultRedisScript<List> slidingWindowScript;

    private SlidingWindowRateLimiter rateLimiter;

    private final RateLimitPolicy policy = new RateLimitPolicy(
            "policy-2",
            "Sliding Window Policy",
            AlgorithmType.SLIDING_WINDOW,
            5,
            60,
            true
    );

    private final RateLimitRequest request = new RateLimitRequest("CLIENT", "Demo:Key", "/api/orders");

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-04-05T10:00:30Z"), ZoneOffset.UTC);
        rateLimiter = new SlidingWindowRateLimiter(stringRedisTemplate, slidingWindowScript, fixedClock);
    }

    @Test
    void shouldAllowRequestWhenSlidingWindowHasCapacity() {
        long nowMillis = Instant.parse("2026-04-05T10:00:30Z").toEpochMilli();
        long windowStartMillis = nowMillis - 60_000L;

        when(stringRedisTemplate.execute(
                eq(slidingWindowScript),
                anyList(),
                eq(String.valueOf(windowStartMillis)),
                eq("5"),
                eq(String.valueOf(nowMillis)),
                argThat(member -> ((String) member).startsWith(nowMillis + "-")),
                eq("61")
        )).thenReturn(List.of(1L, 3L));

        RateLimitResult result = rateLimiter.limit(request, policy);

        assertTrue(result.allowed());
        assertEquals(3, result.remaining());
        assertEquals(0, result.retryAfterSeconds());
        assertEquals("Request allowed in current sliding window", result.message());

        verify(stringRedisTemplate).execute(
                eq(slidingWindowScript),
                eq(List.of("rateshield:sliding:client:demo_key:/api/orders")),
                eq(String.valueOf(windowStartMillis)),
                eq("5"),
                eq(String.valueOf(nowMillis)),
                argThat(member -> ((String) member).startsWith(nowMillis + "-")),
                eq("61")
        );
    }

    @Test
    void shouldBlockRequestWhenSlidingWindowIsFull() {
        long nowMillis = Instant.parse("2026-04-05T10:00:30Z").toEpochMilli();
        long windowStartMillis = nowMillis - 60_000L;

        when(stringRedisTemplate.execute(
                eq(slidingWindowScript),
                anyList(),
                eq(String.valueOf(windowStartMillis)),
                eq("5"),
                eq(String.valueOf(nowMillis)),
                argThat(member -> ((String) member).startsWith(nowMillis + "-")),
                eq("61")
        )).thenReturn(List.of(0L, 0L));

        RateLimitResult result = rateLimiter.limit(request, policy);

        assertFalse(result.allowed());
        assertEquals(0, result.remaining());
        assertEquals(60, result.retryAfterSeconds());
        assertEquals("Rate limit exceeded for the current sliding window", result.message());
    }

    @Test
    void shouldThrowWhenSlidingWindowScriptReturnsInvalidResponse() {
        long nowMillis = Instant.parse("2026-04-05T10:00:30Z").toEpochMilli();
        long windowStartMillis = nowMillis - 60_000L;

        when(stringRedisTemplate.execute(
                eq(slidingWindowScript),
                anyList(),
                eq(String.valueOf(windowStartMillis)),
                eq("5"),
                eq(String.valueOf(nowMillis)),
                argThat(member -> ((String) member).startsWith(nowMillis + "-")),
                eq("61")
        )).thenReturn(List.of(1L));

        assertThrows(IllegalStateException.class, () -> rateLimiter.limit(request, policy));
    }
}
