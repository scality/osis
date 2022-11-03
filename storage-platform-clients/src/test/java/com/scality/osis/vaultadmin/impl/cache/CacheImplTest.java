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

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CacheImplTest {

    private static CacheImpl<String, String> cacheImplUnderTest;

    @BeforeAll
    public static void setUp() {
        cacheImplUnderTest = new CacheImpl<>(1, 30000L);
    }

    @Test
    @Order(1)
    void testPut() {
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
    void testGet() {
        // Setup
        final String key = "Key1";

        // Run the test
        final String result = cacheImplUnderTest.get(key);

        // Verify the results
        assertEquals("Value", result);
    }

    @Test
    @Order(3)
    void testPutWithQuickExpiry() throws InterruptedException {
        // Setup
        final String key = "Key3";
        final String value = "Value3";

        final CacheImpl<String, String> cacheImplUnderTest1 = new CacheImpl<>(1, 200);
        // Run the test
        final String result = cacheImplUnderTest1.put(key, value);

        // Verify the results
        Thread.sleep(205);
        assertEquals(value, result);
        assertNull(cacheImplUnderTest1.get(key));
    }

    @Test
    @Order(4)
    void testRemove() {
        // Setup
        final String key = "Key1";

        // Run the test
        final String result = cacheImplUnderTest.remove(key);

        // Verify the results
        assertEquals("Value", result);
    }

    @Test
    @Order(5)
    void testSize() {
        // Setup

        // Run the test
        cacheImplUnderTest.put("Key1", "Value");
        // Run the test
        final long result = cacheImplUnderTest.size();

        // Verify the results
        assertThat(result).isEqualTo(1);
    }

    @Test
    @Order(6)
    void testToString() {
        // Setup

        // Run the test
        final String result = cacheImplUnderTest.toString();

        // Verify the results
        assertThat(result).isEqualTo("CacheImpl{internalCache={Key1=Value}}");
    }

    @Test
    @Order(7)
    void testClear() {
        // Setup

        // Run the test
        cacheImplUnderTest.clear();

        // Verify the results
        assertEquals(0, cacheImplUnderTest.size());
    }

    @Test
    void testRemove2() {
        // Setup
        final String key = "KeyNull";

        // Run the test with invalid key
        final String result = cacheImplUnderTest.remove(key);

        // Verify the results
        assertNull(result);
    }

    @Test
    void testIllegalMaxCapacity() {
        // Setup
        // Run the test
        // Verify the results
        assertThrows(IllegalArgumentException.class, () -> {
            new CacheImpl<>(-3);
        });
    }

    @Test
    void testIllegalExpirationTime() {
        // Setup
        // Run the test
        // Verify the results
        assertThrows(IllegalArgumentException.class, () -> {
            new CacheImpl<>(1, -100L);
        });
    }

    @Test
    void testPutExistingKey() {
        // Setup
        final String key = "Key2";
        final String value = "Value2";

        // Run the test
        final String value1 = cacheImplUnderTest.put(key, value);
        final String result = cacheImplUnderTest.put(key, value);

        // Verify the results
        assertEquals(value1, result);
    }

    @Test
    @Order(7)
    void testPutMaxKeys() {
        // Setup

        final int maxCapacity = 10;
        final String key = "Key";
        final String value = "Value";

        final CacheImpl<String, String> cacheImplUnderTest2 = new CacheImpl<>(maxCapacity);
        for(int index=0; index< maxCapacity; index++){
            cacheImplUnderTest2.put(key + index, value);
        }

        // Insert extra value higher than maxCapacity
        cacheImplUnderTest2.put("newKey", value);

        // Verify the results
        assertEquals(maxCapacity, cacheImplUnderTest2.size());
    }
}
