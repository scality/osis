package com.scality.osis.redis.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ScalityRedisRepositoryTest {

    @Mock
    private HashOperations<String, String, String> mockHashOperations;

    @InjectMocks
    private ScalityRedisRepository scalityRedisRepositoryUnderTest;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(scalityRedisRepositoryUnderTest, "hashOperations", mockHashOperations);
    }

    @Test
    void testSave() {
        // Setup

        // Run the test
        scalityRedisRepositoryUnderTest.save("key", "value");

        // Verify the results
        verify(mockHashOperations).put(any(), any(), any());
    }

    @Test
    void testGet() {
        // Setup
        when(mockHashOperations.get(any(), any())).thenReturn("value");

        // Run the test
        final String result = (String) scalityRedisRepositoryUnderTest.get("key");

        // Verify the results
        assertEquals("value", result);
    }

    @Test
    void testDelete() {
        // Setup

        // Run the test
        scalityRedisRepositoryUnderTest.delete("key");

        // Verify the results
        verify(mockHashOperations).delete(any(), any());
    }

    @Test
    void testHasKey() {
        // Setup

        // Run the test
        scalityRedisRepositoryUnderTest.hasKey("key");

        // Verify the results
        verify(mockHashOperations).hasKey(any(), any());
    }
}
