package com.scality.osis.configuration;

import com.scality.osis.vaultadmin.VaultAdmin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ScalityRestConfigTest {

    private ScalityRestConfig scalityRestConfigUnderTest;

    @BeforeEach
    public void setUp() {
        scalityRestConfigUnderTest = new ScalityRestConfig();
    }

    @Test
    public void testSimpleClientHttpRequestFactory() {
        // Setup

        // Run the test
        final ClientHttpRequestFactory result = scalityRestConfigUnderTest.simpleClientHttpRequestFactory();

        // Verify the results
        assertNotNull(result,"result should not be null");
    }

    @Test
    public void testGetVaultAdmin() {
        // Setup private fields in the object with test values
        ReflectionTestUtils.setField(scalityRestConfigUnderTest, "vaultAccessKey", "accesskey");
        ReflectionTestUtils.setField(scalityRestConfigUnderTest, "vaultSecretKey", "secretkey");
        ReflectionTestUtils.setField(scalityRestConfigUnderTest, "vaultEndpoint", "http://127.0.0.1");
        // Run the test
        final VaultAdmin result = scalityRestConfigUnderTest.getVaultAdmin();

        // Verify the results
        assertNotNull(result,"result should not be null");


    }
}
