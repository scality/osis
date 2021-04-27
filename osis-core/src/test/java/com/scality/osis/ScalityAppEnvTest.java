package com.scality.osis;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.core.env.Environment;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ScalityAppEnvTest {
    private static final String TEST_RESULT= "result";
    private static final String TEST_RESULT_NULL_ERR= "result should be null";

    @Mock
    private Environment mockEnv;

    @InjectMocks
    private ScalityAppEnv appEnvUnderTest;

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void testGetVaultEndpoint() {
        // Setup
        when(mockEnv.getProperty("osis.scality.vault.endpoint")).thenReturn(TEST_RESULT);

        // Run the test
        final String result = appEnvUnderTest.getPlatformEndpoint();

        // Verify the results
        assertEquals(result, TEST_RESULT, "testGetVaultEndpoint should be equal");
    }

    @Test
    public void testGetVaultEndpointEnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("osis.scality.vault.endpoint")).thenReturn(null);

        // Run the test
        final String result = appEnvUnderTest.getPlatformEndpoint();

        // Verify the results
        assertNull(result, TEST_RESULT_NULL_ERR);
    }

    @Test
    public void testGetVaultAccessKey() {
        // Setup
        when(mockEnv.getProperty("osis.scality.vault.access-key")).thenReturn(TEST_RESULT);

        // Run the test
        final String result = appEnvUnderTest.getPlatformAccessKey();

        // Verify the results
        assertEquals(result, TEST_RESULT,"testGetVaultAccessKey failed");
    }

    @Test
    public void testGetVaultAccessKeyEnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("osis.scality.vault.access-key")).thenReturn(null);

        // Run the test
        final String result = appEnvUnderTest.getPlatformAccessKey();

        // Verify the results
        assertNull(result, TEST_RESULT_NULL_ERR);
    }

    @Test
    public void testGetVaultSecretKey() {
        // Setup
        when(mockEnv.getProperty("osis.scality.vault.secret-key")).thenReturn(TEST_RESULT);

        // Run the test
        final String result = appEnvUnderTest.getPlatformSecretKey();

        // Verify the results
        assertEquals(result, TEST_RESULT, "testGetVaultSecretKey failed");
    }

    @Test
    public void testGetVaultSecretKeyEnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("osis.scality.vault.secret-key")).thenReturn(null);

        // Run the test
        final String result = appEnvUnderTest.getPlatformSecretKey();

        // Verify the results
        assertNull(result, TEST_RESULT_NULL_ERR);
    }

    @Test
    public void testGetS3InterfaceEndpoint() {
        // Setup
        when(mockEnv.getProperty("osis.scality.vaultS3Interface.endpoint")).thenReturn(TEST_RESULT);

        // Run the test
        final String result = appEnvUnderTest.getS3InterfaceEndpoint();

        // Verify the results
        assertEquals(result, TEST_RESULT, "testGetS3InterfaceEndpoint failed");
    }

    @Test
    public void testGetS3InterfaceEndpointEnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("osis.scality.vaultS3Interface.endpoint")).thenReturn(null);

        // Run the test
        final String result = appEnvUnderTest.getS3InterfaceEndpoint();

        // Verify the results
        assertNull(result, TEST_RESULT_NULL_ERR);
    }

    @Test
    public void testGetConsoleEndpoint() {
        // Setup
        when(mockEnv.getProperty("osis.scality.console.endpoint")).thenReturn(TEST_RESULT);

        // Run the test
        final String result = appEnvUnderTest.getConsoleEndpoint();

        // Verify the results
        assertEquals(result, TEST_RESULT, "testGetConsoleEndpoint failed");
    }

    @Test
    public void testGetConsoleEndpointEnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("osis.scality.console.endpoint")).thenReturn(null);

        // Run the test
        final String result = appEnvUnderTest.getConsoleEndpoint();

        // Verify the results
        assertNull(result, TEST_RESULT_NULL_ERR);
    }

    @Test
    public void testGetStorageInfo() {
        // Setup
        when(mockEnv.getProperty("osis.scality.storage-classes")).thenReturn(TEST_RESULT);

        // Run the test
        final List<String> result = appEnvUnderTest.getStorageInfo();

        // Verify the results
        assertEquals(result, Arrays.asList(TEST_RESULT), "testGetStorageInfo failed");
    }

    @Test
    public void testGetStorageInfoEnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("osis.scality.storage-classes")).thenReturn(null);

        // Run the test
        final List<String> result = appEnvUnderTest.getStorageInfo();

        // Verify the results
        assertEquals(result, Arrays.asList("standard"), "testGetStorageInfoEnvironmentReturnsNull failed");
    }

    @Test
    public void testGetRegionInfo() {
        // Setup
        when(mockEnv.getProperty("osis.scality.region")).thenReturn("region1,region2");

        // Run the test
        final List<String> result = appEnvUnderTest.getRegionInfo();

        // Verify the results
        assertEquals(result, Arrays.asList("region1","region2"), "testGetRegionInfo failed");
    }

    @Test
    public void testGetRegionInfoEnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("osis.scality.region")).thenReturn(null);

        // Run the test
        final List<String> result = appEnvUnderTest.getRegionInfo();

        // Verify the results
        assertEquals(result, Arrays.asList("us-east-1"), "testGetRegionInfoEnvironmentReturnsNull failed");
    }

    @Test
    public void testGetPlatformName() {
        // Setup
        when(mockEnv.getProperty("osis.scality.name")).thenReturn("scality");

        // Run the test
        final String result = appEnvUnderTest.getPlatformName();

        // Verify the results
        assertEquals(result, "scality", "testGetPlatformName failed");
    }

    @Test
    public void testGetPlatformNameEnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("osis.scality.name")).thenReturn(null);

        // Run the test
        final String result = appEnvUnderTest.getPlatformName();

        // Verify the results
        assertNull(result, TEST_RESULT_NULL_ERR);
    }

    @Test
    public void testGetPlatformVersion() {
        // Setup
        when(mockEnv.getProperty("osis.scality.version")).thenReturn("v1");

        // Run the test
        final String result = appEnvUnderTest.getPlatformVersion();

        // Verify the results
        assertEquals(result, "v1", "testGetPlatformVersion failed");
    }

    @Test
    public void testGetPlatformVersionEnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("osis.scality.version")).thenReturn(null);

        // Run the test
        final String result = appEnvUnderTest.getPlatformVersion();

        // Verify the results
        assertNull(result, TEST_RESULT_NULL_ERR);
    }

    @Test
    public void testGetApiVersion() {
        // Setup
        when(mockEnv.getProperty("osis.api.version")).thenReturn("v1");

        // Run the test
        final String result = appEnvUnderTest.getApiVersion();

        // Verify the results
        assertEquals(result, "v1", "testGetApiVersion failed");
    }

    @Test
    public void testGetApiVersionEnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("osis.api.version")).thenReturn(null);

        // Run the test
        final String result = appEnvUnderTest.getApiVersion();

        // Verify the results
        assertNull(result, TEST_RESULT_NULL_ERR);
    }

    @Test
    public void testIsApiTokenEnabled() {
        // Setup
        when(mockEnv.getProperty("security.jwt.enabled")).thenReturn("true");

        // Run the test
        final boolean result = appEnvUnderTest.isApiTokenEnabled();

        // Verify the results
        assertTrue(result, "testIsApiTokenEnabled failed");
    }

    @Test
    public void testIsApiTokenEnabledEnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("security.jwt.enabled")).thenReturn(null);

        // Run the test
        final boolean result = appEnvUnderTest.isApiTokenEnabled();

        // Verify the results
        assertFalse(result, "testIsApiTokenEnabledEnvironmentReturnsNull failed");
    }

    @Test
    public void testGetTokenIssuer() {
        // Setup
        when(mockEnv.getProperty("security.jwt.token-issuer")).thenReturn(TEST_RESULT);

        // Run the test
        final String result = appEnvUnderTest.getTokenIssuer();

        // Verify the results
        assertEquals(result, TEST_RESULT, "testGetTokenIssuer failed");
    }

    @Test
    public void testGetTokenIssuerEnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("security.jwt.token-issuer")).thenReturn(null);

        // Run the test
        final String result = appEnvUnderTest.getTokenIssuer();

        // Verify the results
        assertNull(result, TEST_RESULT_NULL_ERR);
    }

    @Test
    public void testGetAccessTokenExpirationTime() {
        // Setup
        when(mockEnv.getProperty("security.jwt.access-token-expiration-time")).thenReturn("1000");

        // Run the test
        final int result = appEnvUnderTest.getAccessTokenExpirationTime();

        // Verify the results
        assertEquals(result, 1000, "testGetAccessTokenExpirationTime failed");
    }

    @Test
    public void testGetAccessTokenExpirationTimeEnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("security.jwt.access-token-expiration-time")).thenReturn(null);

        // Run the test
        // Verify the results
        assertThrows(NumberFormatException.class, ()->appEnvUnderTest.getAccessTokenExpirationTime(), "testGetAccessTokenExpirationTimeEnvironmentReturnsNull should throw NumberFormatException");
    }

    @Test
    public void testGetTokenSigningKey() {
        // Setup
        when(mockEnv.getProperty("security.jwt.token-signing-key")).thenReturn(TEST_RESULT);

        // Run the test
        final String result = appEnvUnderTest.getTokenSigningKey();

        // Verify the results
        assertEquals(result, TEST_RESULT, "testGetTokenSigningKey failed");
    }

    @Test
    public void testGetTokenSigningKeyEnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("security.jwt.token-signing-key")).thenReturn(null);

        // Run the test
        final String result = appEnvUnderTest.getTokenSigningKey();

        // Verify the results
        assertNull(result, TEST_RESULT_NULL_ERR);
    }

    @Test
    public void testGetRefreshTokenExpirationTime() {
        // Setup
        when(mockEnv.getProperty("security.jwt.refresh_token_expiration_time")).thenReturn("1000");

        // Run the test
        final int result = appEnvUnderTest.getRefreshTokenExpirationTime();

        // Verify the results
        assertEquals(result, 1000, "testGetRefreshTokenExpirationTime failed");
    }

    @Test
    public void testGetRefreshTokenExpirationTimeEnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("security.jwt.refresh_token_expiration_time")).thenReturn(null);

        // Run the test
        // Verify the results
        assertThrows(NumberFormatException.class, ()->appEnvUnderTest.getRefreshTokenExpirationTime(), "testGetRefreshTokenExpirationTimeEnvironmentReturnsNull should throw NumberFormatException");
    }

    @Test
    public void testGetAssumeRoleName() {
        // Setup
        when(mockEnv.getProperty("osis.scality.vault.role.name")).thenReturn("osis");

        // Run the test
        final String result = appEnvUnderTest.getAssumeRoleName();

        // Verify the results
        assertEquals(result, "osis", "testGetAssumeRoleName failed");
    }
}
