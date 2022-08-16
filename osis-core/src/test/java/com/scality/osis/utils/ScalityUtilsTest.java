package com.scality.osis.utils;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static com.scality.osis.utils.ScalityConstants.*;
import static com.scality.osis.utils.ScalityTestUtils.SAMPLE_TENANT_ID;
import static com.scality.osis.utils.ScalityTestUtils.TEST_USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ScalityUtilsTest {

    @Test
    public void testIsValidUUID() {
        assertThat(ScalityUtils.isValidUUID(UUID.randomUUID().toString())).isTrue();
    }

    @Test
    public void testIsInValidUUID() {
        assertThat(ScalityUtils.isValidUUID("str")).isFalse();
    }

    @Test
    public void testParseFilter() {
        // Setup
        final String filter = TENANT_ID_PREFIX + SAMPLE_TENANT_ID + FILTER_SEPARATOR + USER_ID_PREFIX + TEST_USER_ID ;

        // Run the test
        final Map<String, String> filterMap = ScalityUtils.parseFilter(filter);

        // Verify the results
        assertEquals(SAMPLE_TENANT_ID, filterMap.get(OSIS_TENANT_ID));
        assertEquals(TEST_USER_ID, filterMap.get(OSIS_USER_ID));
    }

    @Test
    public void testParseEmptyFilter() {
        // Setup

        // Run the test
        final Map<String, String> filterMap = ScalityUtils.parseFilter("");

        // Verify the results
        assertTrue(filterMap.isEmpty());
    }
}
