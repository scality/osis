package com.scality.osis.vaultadmin.impl.cache;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CacheImplTest {

    private static CacheImpl<String, String> cacheImplUnderTest;

    @BeforeAll
    public static void setUp() {
        cacheImplUnderTest = new CacheImpl<>(3);
    }

    @Test
    @Order(1)
    public void testPut1() {
        // Setup
        final String key = "Key1";
        final String value = "Value";

        // Run the test
        final String result = cacheImplUnderTest.put(key, value);

        // Verify the results
        assertEquals(value, result);
    }

    @Test
    @Order(2)
    public void testPutWithExpiry() {
        // Setup
        final String key = "Key2";
        final String value = "Value2";

        // Run the test
        final String result = cacheImplUnderTest.put(key, value, 30L);

        // Verify the results
        assertEquals(value, result);
    }

    @Test
    @Order(3)
    public void testGet() {
        // Setup
        final String key = "Key1";

        // Run the test
        final String result = cacheImplUnderTest.get(key);

        // Verify the results
        assertEquals("Value", result);
    }

    @Test
    @Order(4)
    public void testPutWithQuickExpiry() throws InterruptedException {
        // Setup
        final String key = "Key3";
        final String value = "Value3";

        // Run the test
        final String result = cacheImplUnderTest.put(key, value, 1);

        // Verify the results
        Thread.sleep(1);
        assertEquals(value, result);
        assertNull(cacheImplUnderTest.get(key));
    }

    @Test
    @Order(5)
    public void testRemove() {
        // Setup
        final String key = "Key2";

        // Run the test
        final String result = cacheImplUnderTest.remove(key);

        // Verify the results
        assertEquals("Value2", result);
    }

    @Test
    @Order(6)
    public void testSize() {
        // Setup

        // Run the test
        final long result = cacheImplUnderTest.size();

        // Verify the results
        assertThat(result).isEqualTo(1);
    }

    @Test
    @Order(7)
    public void testContainsKey() {
        // Setup
        final String key = "Key1";

        // Run the test
        final boolean result = cacheImplUnderTest.containsKey(key);

        // Verify the results
        assertTrue(result);
    }

    @Test
    @Order(8)
    public void testToString() {
        // Setup

        // Run the test
        final String result = cacheImplUnderTest.toString();

        // Verify the results
        assertThat(result).isEqualTo("CacheImpl{internalCache={Key1=Value}}");
    }

    @Test
    @Order(9)
    public void testClear() {
        // Setup

        // Run the test
        cacheImplUnderTest.clear();

        // Verify the results
        assertEquals(0, cacheImplUnderTest.size());
    }

    @Test
    public void testRemove2() {
        // Setup
        final String key = "KeyNull";

        // Run the test with invalid key
        final String result = cacheImplUnderTest.remove(key);

        // Verify the results
        assertNull(result);
    }

    @Test
    public void testIllegalMaxCapacity() {
        // Setup
        // Run the test
        // Verify the results
        assertThrows(IllegalArgumentException.class, () -> {
            new CacheImpl<>(-3);
        });
    }

    @Test
    public void testPutExistingKey() {
        // Setup
        final String key = "Key2";
        final String value = "Value2";

        // Run the test
        final String value1 = cacheImplUnderTest.put(key, value, 30L);
        final String result = cacheImplUnderTest.put(key, value, 30L);

        // Verify the results
        assertEquals(value1, result);
    }

    @Test
    @Order(7)
    public void testPutMaxKeys() {
        // Setup

        final int maxCapacity = 10;
        final String key = "Key";
        final String value = "Value";

        final CacheImpl<String, String> cacheImplUnderTest2 = new CacheImpl<>(maxCapacity);
        for(int index=0; index< maxCapacity; index++){
            cacheImplUnderTest2.put(key + index, value, 30000);
        }

        // Insert extra value higher than maxCapacity
        cacheImplUnderTest2.put("newKey", value, 30L);

        // Verify the results
        assertEquals(maxCapacity, cacheImplUnderTest2.size());
    }
}
