package com.scality.osis.utapiclient.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MetricsDataTest {

    private MetricsData metricsDataUnderTest;

    @BeforeEach
    public void setUp() {
        //Set up Original data
        metricsDataUnderTest = new MetricsData();
        metricsDataUnderTest.accountId = "accountId";
        metricsDataUnderTest.incomingBytes = 0L;
        metricsDataUnderTest.outgoingBytes = 0L;
        metricsDataUnderTest.numberOfObjects = Arrays.asList(0L, 0L);
        metricsDataUnderTest.storageUtilized = Arrays.asList(0L, 0L);
        metricsDataUnderTest.operations = Map.of("s3:ListObject", 0);
        metricsDataUnderTest.timeRange = List.of(0L);
    }

    @Test
    void testEqualsAndHashCode() {
        // Setup expected data
        final MetricsData data = new MetricsData();
        data.setAccountId("accountId");
        data.setIncomingBytes(0L);
        data.setOutgoingBytes(0L);
        data.setNumberOfObjects(Arrays.asList(0L, 0L));
        data.setStorageUtilized(Arrays.asList(0L, 0L));
        data.setOperations(Map.of("s3:ListObject", 0));
        data.setTimeRange(List.of(0L));
        // Run the test
        assertEquals(metricsDataUnderTest, data);
        assertEquals(metricsDataUnderTest.hashCode(), data.hashCode());
    }

    @Test
    void testToString() {

        // Run the test
        final String result = metricsDataUnderTest.toString();

        // Verify the results
        assertNotNull( result);
    }
}
