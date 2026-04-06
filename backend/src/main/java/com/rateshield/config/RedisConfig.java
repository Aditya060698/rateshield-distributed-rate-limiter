package com.rateshield.config;

import java.time.Clock;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public RedisSerializer<String> redisKeySerializer() {
        return new StringRedisSerializer();
    }

    @Bean
    public DefaultRedisScript<List> fixedWindowScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setResultType(List.class);
        script.setScriptText("""
                local current = redis.call('INCR', KEYS[1])
                if current == 1 then
                    redis.call('EXPIRE', KEYS[1], ARGV[1])
                end

                local limit = tonumber(ARGV[2])
                local remaining = limit - current
                if remaining < 0 then
                    remaining = 0
                end

                local allowed = 0
                if current <= limit then
                    allowed = 1
                end

                return {allowed, remaining}
                """);
        return script;
    }

    @Bean
    public DefaultRedisScript<List> slidingWindowScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setResultType(List.class);
        script.setScriptText("""
                redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', ARGV[1])

                local current = redis.call('ZCARD', KEYS[1])
                local limit = tonumber(ARGV[2])
                if current >= limit then
                    return {0, 0}
                end

                redis.call('ZADD', KEYS[1], ARGV[3], ARGV[4])
                redis.call('EXPIRE', KEYS[1], ARGV[5])

                local remaining = limit - (current + 1)
                if remaining < 0 then
                    remaining = 0
                end

                return {1, remaining}
                """);
        return script;
    }

    @Bean
    public DefaultRedisScript<List> tokenBucketScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setResultType(List.class);
        script.setScriptText("""
                local capacity = tonumber(ARGV[1])
                local refill_rate_per_millis = tonumber(ARGV[2])
                local now_millis = tonumber(ARGV[3])
                local ttl_seconds = tonumber(ARGV[4])

                local current_tokens = tonumber(redis.call('HGET', KEYS[1], 'tokens'))
                local last_refill = tonumber(redis.call('HGET', KEYS[1], 'lastRefillEpochMillis'))

                if current_tokens == nil or last_refill == nil then
                    current_tokens = capacity
                    last_refill = now_millis
                end

                local elapsed = now_millis - last_refill
                if elapsed < 0 then
                    elapsed = 0
                end

                local replenished = elapsed * refill_rate_per_millis
                current_tokens = math.min(capacity, current_tokens + replenished)

                if current_tokens < 1 then
                    local missing = 1 - current_tokens
                    local retry_after_millis = math.ceil(missing / refill_rate_per_millis)
                    local retry_after_seconds = math.max(math.ceil(retry_after_millis / 1000), 1)

                    redis.call('HSET', KEYS[1], 'tokens', tostring(current_tokens))
                    redis.call('HSET', KEYS[1], 'lastRefillEpochMillis', tostring(now_millis))
                    redis.call('EXPIRE', KEYS[1], ttl_seconds)

                    return {0, tostring(math.floor(current_tokens)), tostring(retry_after_seconds)}
                end

                current_tokens = current_tokens - 1
                redis.call('HSET', KEYS[1], 'tokens', tostring(current_tokens))
                redis.call('HSET', KEYS[1], 'lastRefillEpochMillis', tostring(now_millis))
                redis.call('EXPIRE', KEYS[1], ttl_seconds)

                return {1, tostring(math.floor(current_tokens)), '0'}
                """);
        return script;
    }
}
