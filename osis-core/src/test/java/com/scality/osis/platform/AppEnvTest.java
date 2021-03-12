package com.scality.osis.platform;

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

public class AppEnvTest {
    private static final String TEST_RESULT= "result";
    private static final String TEST_RESULT_NULL_ERR= "result should be null";

    @Mock
    private Environment mockEnv;

    @InjectMocks
    private AppEnv appEnvUnderTest;

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void testGetVaultEndpoint() {
        // Setup
        when(mockEnv.getProperty("osis.scality.vault.endpoint")).thenReturn(TEST_RESULT);

        // Run the test
        final String result = appEnvUnderTest.getVaultEndpoint();

        // Verify the results
        assertEquals(result, TEST_RESULT, "testGetVaultEndpoint should be equal");
    }

    @Test
    public void testGetVaultEndpoint_EnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("osis.scality.vault.endpoint")).thenReturn(null);

        // Run the test
        final String result = appEnvUnderTest.getVaultEndpoint();

        // Verify the results
        assertNull(result, TEST_RESULT_NULL_ERR);
    }

    @Test
    public void testGetVaultAccessKey() {
        // Setup
        when(mockEnv.getProperty("osis.scality.vault.username")).thenReturn(TEST_RESULT);

        // Run the test
        final String result = appEnvUnderTest.getVaultAccessKey();

        // Verify the results
        assertEquals(result, TEST_RESULT,"testGetVaultAccessKey failed");
    }

    @Test
    public void testGetVaultAccessKey_EnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("osis.scality.vault.username")).thenReturn(null);

        // Run the test
        final String result = appEnvUnderTest.getVaultAccessKey();

        // Verify the results
        assertNull(result, TEST_RESULT_NULL_ERR);
    }

    @Test
    public void testGetVaultSecretKey() {
        // Setup
        when(mockEnv.getProperty("osis.scality.vault.password")).thenReturn(TEST_RESULT);

        // Run the test
        final String result = appEnvUnderTest.getVaultSecretKey();

        // Verify the results
        assertEquals(result, TEST_RESULT, "testGetVaultSecretKey failed");
    }

    @Test
    public void testGetVaultSecretKey_EnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("osis.scality.vault.password")).thenReturn(null);

        // Run the test
        final String result = appEnvUnderTest.getVaultSecretKey();

        // Verify the results
        assertNull(result, TEST_RESULT_NULL_ERR);
    }

    @Test
    public void testGetS3Endpoint() {
        // Setup
        when(mockEnv.getProperty("osis.scality.s3.endpoint")).thenReturn(TEST_RESULT);

        // Run the test
        final String result = appEnvUnderTest.getS3Endpoint();

        // Verify the results
        assertEquals(result, TEST_RESULT, "testGetS3Endpoint failed");
    }

    @Test
    public void testGetS3Endpoint_EnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("osis.scality.s3.endpoint")).thenReturn(null);

        // Run the test
        final String result = appEnvUnderTest.getS3Endpoint();

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
    public void testGetConsoleEndpoint_EnvironmentReturnsNull() {
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
    public void testGetStorageInfo_EnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("osis.scality.storage-classes")).thenReturn(null);

        // Run the test
        final List<String> result = appEnvUnderTest.getStorageInfo();

        // Verify the results
        assertEquals(result, Arrays.asList("standard"), "testGetStorageInfo_EnvironmentReturnsNull failed");
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
    public void testGetRegionInfo_EnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("osis.scality.region")).thenReturn(null);

        // Run the test
        final List<String> result = appEnvUnderTest.getRegionInfo();

        // Verify the results
        assertEquals(result, Arrays.asList("default"), "testGetRegionInfo_EnvironmentReturnsNull failed");
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
    public void testGetPlatformName_EnvironmentReturnsNull() {
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
    public void testGetPlatformVersion_EnvironmentReturnsNull() {
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
    public void testGetApiVersion_EnvironmentReturnsNull() {
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
    public void testIsApiTokenEnabled_EnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("security.jwt.enabled")).thenReturn(null);

        // Run the test
        final boolean result = appEnvUnderTest.isApiTokenEnabled();

        // Verify the results
        assertFalse(result, "testIsApiTokenEnabled_EnvironmentReturnsNull failed");
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
    public void testGetTokenIssuer_EnvironmentReturnsNull() {
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
    public void testGetAccessTokenExpirationTime_EnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("security.jwt.access-token-expiration-time")).thenReturn(null);

        // Run the test
        // Verify the results
        assertThrows(NumberFormatException.class, ()->appEnvUnderTest.getAccessTokenExpirationTime(), "testGetAccessTokenExpirationTime_EnvironmentReturnsNull should throw NumberFormatException");
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
    public void testGetTokenSigningKey_EnvironmentReturnsNull() {
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
    public void testGetRefreshTokenExpirationTime_EnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("security.jwt.refresh_token_expiration_time")).thenReturn(null);

        // Run the test
        // Verify the results
        assertThrows(NumberFormatException.class, ()->appEnvUnderTest.getRefreshTokenExpirationTime(), "testGetRefreshTokenExpirationTime_EnvironmentReturnsNull should throw NumberFormatException");
    }
}
