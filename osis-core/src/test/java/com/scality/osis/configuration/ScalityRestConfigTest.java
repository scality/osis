package com.scality.osis.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.client.ClientHttpRequestFactory;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ScalityRestConfigTest {

    private ScalityRestConfig scalityRestConfigUnderTest;

    @BeforeEach
    public void setUp() {
        scalityRestConfigUnderTest = new ScalityRestConfig();
    }

    @Test
    void testSimpleClientHttpRequestFactory() {
        // Setup

        // Run the test
        final ClientHttpRequestFactory result = scalityRestConfigUnderTest.simpleClientHttpRequestFactory();

        // Verify the results
        assertNotNull(result,"result should not be null");
    }

}
