package com.scality.osis.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ScalityRedisRepositoryTest {

    @Mock
    private StringRedisTemplate mockRedisTemplate;

    @Mock
    private HashOperations mockHashOperations;

    @InjectMocks
    private ScalityRedisRepository scalityRedisRepositoryUnderTest;

    @BeforeEach
    public void setUp() {
        initMocks(this);

        when(mockRedisTemplate.opsForHash()).thenReturn(mockHashOperations);
        ReflectionTestUtils.setField(scalityRedisRepositoryUnderTest, "redisTemplate", mockRedisTemplate);
    }

    @Test
    public void testSave() {
        // Setup

        // Run the test
        scalityRedisRepositoryUnderTest.save("key", "value");

        // Verify the results
        verify(mockHashOperations).put(any(), any(), any());
    }

    @Test
    public void testGet() {
        // Setup
        when(mockHashOperations.get(any(), any())).thenReturn("value");

        // Run the test
        final String result = scalityRedisRepositoryUnderTest.get("key");

        // Verify the results
        assertEquals("value", result);
    }

    @Test
    public void testDelete() {
        // Setup

        // Run the test
        scalityRedisRepositoryUnderTest.delete("key");

        // Verify the results
        verify(mockHashOperations).delete(any(), any());
    }

    @Test
    public void testHasKey() {
        // Setup

        // Run the test
        scalityRedisRepositoryUnderTest.hasKey("key");

        // Verify the results
        verify(mockHashOperations).hasKey(any(), any());
    }
}
