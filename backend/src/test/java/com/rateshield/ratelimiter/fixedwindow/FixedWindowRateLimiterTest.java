package com.rateshield.ratelimiter.fixedwindow;

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
class FixedWindowRateLimiterTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private DefaultRedisScript<List> fixedWindowScript;

    private FixedWindowRateLimiter rateLimiter;

    private final RateLimitPolicy policy = new RateLimitPolicy(
            "policy-1",
            "Fixed Window Policy",
            AlgorithmType.FIXED_WINDOW,
            5,
            60,
            true
    );

    private final RateLimitRequest request = new RateLimitRequest("USER", "Client:123", "/api/search");

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-04-05T10:00:30Z"), ZoneOffset.UTC);
        rateLimiter = new FixedWindowRateLimiter(stringRedisTemplate, fixedWindowScript, fixedClock);
    }

    @Test
    void shouldAllowRequestWhenRedisReturnsAllowedDecision() {
        when(stringRedisTemplate.execute(eq(fixedWindowScript), anyList(), eq("31"), eq("5")))
                .thenReturn(List.of(1L, 4L));

        RateLimitResult result = rateLimiter.limit(request, policy);

        assertTrue(result.allowed());
        assertEquals(4, result.remaining());
        assertEquals(0, result.retryAfterSeconds());
        assertEquals("Request allowed in current fixed window", result.message());

        verify(stringRedisTemplate).execute(
                eq(fixedWindowScript),
                eq(List.of("rateshield:fixed:user:client_123:/api/search:29589720")),
                eq("31"),
                eq("5")
        );
    }

    @Test
    void shouldBlockRequestWhenRedisReturnsRejectedDecision() {
        when(stringRedisTemplate.execute(eq(fixedWindowScript), anyList(), eq("31"), eq("5")))
                .thenReturn(List.of(0L, 0L));

        RateLimitResult result = rateLimiter.limit(request, policy);

        assertFalse(result.allowed());
        assertEquals(0, result.remaining());
        assertEquals(30, result.retryAfterSeconds());
        assertEquals("Rate limit exceeded for the current fixed window", result.message());
    }

    @Test
    void shouldThrowWhenRedisReturnsInvalidResponse() {
        when(stringRedisTemplate.execute(eq(fixedWindowScript), anyList(), eq("31"), eq("5")))
                .thenReturn(null);

        assertThrows(IllegalStateException.class, () -> rateLimiter.limit(request, policy));
    }
}
