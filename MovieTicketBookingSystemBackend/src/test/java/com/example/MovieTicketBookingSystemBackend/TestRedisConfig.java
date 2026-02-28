package com.example.MovieTicketBookingSystemBackend;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Provides a mocked RedisTemplate for tests when Redis autoconfig is excluded.
 * Uses an in-memory Map so seat lock (setIfAbsent) and isLocked (hasKey) behave correctly.
 */
@TestConfiguration
public class TestRedisConfig {

    private static final Map<String, String> STORAGE = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    @Bean("redisTemplate")
    @Primary
    public RedisTemplate<String, String> redisTemplate() {
        RedisTemplate<String, String> template = mock(RedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(ops);
        when(template.hasKey(anyString())).thenAnswer(inv -> STORAGE.containsKey(inv.getArgument(0)));
        when(ops.setIfAbsent(any(), any(), any(Duration.class)))
                .thenAnswer(inv -> STORAGE.putIfAbsent(inv.getArgument(0), inv.getArgument(1)) == null);
        when(ops.setIfAbsent(any(), any())).thenAnswer(inv -> STORAGE.putIfAbsent(inv.getArgument(0), inv.getArgument(1)) == null);
        return template;
    }
}
