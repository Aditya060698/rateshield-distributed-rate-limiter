package com.rateshield.ratelimiter.tokenbucket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
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
class TokenBucketRateLimiterTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private DefaultRedisScript<List> tokenBucketScript;

    private TokenBucketRateLimiter rateLimiter;

    private final RateLimitPolicy policy = new RateLimitPolicy(
            "policy-3",
            "Token Bucket Policy",
            AlgorithmType.TOKEN_BUCKET,
            5,
            60,
            true
    );

    private final RateLimitRequest request = new RateLimitRequest("API_KEY", "Key:ABC", "/api/payments");

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-04-05T10:00:30Z"), ZoneOffset.UTC);
        rateLimiter = new TokenBucketRateLimiter(stringRedisTemplate, tokenBucketScript, fixedClock);
    }

    @Test
    void shouldAllowRequestWhenBucketHasTokens() {
        long nowMillis = Instant.parse("2026-04-05T10:00:30Z").toEpochMilli();
        String refillRatePerMilli = String.valueOf(5.0d / 60_000.0d);

        when(stringRedisTemplate.execute(
                eq(tokenBucketScript),
                anyList(),
                eq("5.0"),
                eq(refillRatePerMilli),
                eq(String.valueOf(nowMillis)),
                eq("61")
        )).thenReturn(List.of(1L, 4L, 0L));

        RateLimitResult result = rateLimiter.limit(request, policy);

        assertTrue(result.allowed());
        assertEquals(4, result.remaining());
        assertEquals(0, result.retryAfterSeconds());
        assertEquals("Request allowed using token bucket", result.message());

        verify(stringRedisTemplate).execute(
                eq(tokenBucketScript),
                eq(List.of("rateshield:token:api_key:key_abc:/api/payments")),
                eq("5.0"),
                eq(refillRatePerMilli),
                eq(String.valueOf(nowMillis)),
                eq("61")
        );
    }

    @Test
    void shouldBlockRequestWhenBucketIsEmpty() {
        long nowMillis = Instant.parse("2026-04-05T10:00:30Z").toEpochMilli();
        String refillRatePerMilli = String.valueOf(5.0d / 60_000.0d);

        when(stringRedisTemplate.execute(
                eq(tokenBucketScript),
                anyList(),
                eq("5.0"),
                eq(refillRatePerMilli),
                eq(String.valueOf(nowMillis)),
                eq("61")
        )).thenReturn(List.of(0L, 0L, 12L));

        RateLimitResult result = rateLimiter.limit(request, policy);

        assertFalse(result.allowed());
        assertEquals(0, result.remaining());
        assertEquals(12, result.retryAfterSeconds());
        assertEquals("Rate limit exceeded because the token bucket is empty", result.message());
    }

    @Test
    void shouldThrowWhenTokenBucketScriptReturnsInvalidResponse() {
        long nowMillis = Instant.parse("2026-04-05T10:00:30Z").toEpochMilli();
        String refillRatePerMilli = String.valueOf(5.0d / 60_000.0d);

        when(stringRedisTemplate.execute(
                eq(tokenBucketScript),
                anyList(),
                eq("5.0"),
                eq(refillRatePerMilli),
                eq(String.valueOf(nowMillis)),
                eq("61")
        )).thenReturn(List.of(1L, 4L));

        assertThrows(IllegalStateException.class, () -> rateLimiter.limit(request, policy));
    }
}
