package com.scality.osis;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.env.Environment;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ScalityAppEnvTest {
    private static final String TEST_RESULT= "result";
    private static final String TEST_RESULT_NULL_ERR= "result should be null";

    @Mock
    private Environment mockEnv;

    @InjectMocks
    private ScalityAppEnv appEnvUnderTest;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetVaultEndpoint() {
        // Setup
        when(mockEnv.getProperty("osis.scality.vault.endpoint")).thenReturn(TEST_RESULT);

        // Run the test
        final String result = appEnvUnderTest.getPlatformEndpoint();

        // Verify the results
        assertEquals(result, TEST_RESULT, "testGetVaultEndpoint should be equal");
    }

    @Test
    void testGetVaultEndpointEnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("osis.scality.vault.endpoint")).thenReturn(null);

        // Run the test
        final String result = appEnvUnderTest.getPlatformEndpoint();

        // Verify the results
        assertNull(result, TEST_RESULT_NULL_ERR);
    }

    @Test
    void testGetVaultAccessKey() {
        // Setup
        when(mockEnv.getProperty("osis.scality.vault.access-key")).thenReturn(TEST_RESULT);

        // Run the test
        final String result = appEnvUnderTest.getPlatformAccessKey();

        // Verify the results
        assertEquals(result, TEST_RESULT,"testGetVaultAccessKey failed");
    }

    @Test
    void testGetVaultAccessKeyEnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("osis.scality.vault.access-key")).thenReturn(null);

        // Run the test
        final String result = appEnvUnderTest.getPlatformAccessKey();

        // Verify the results
        assertNull(result, TEST_RESULT_NULL_ERR);
    }

    @Test
    void testGetVaultSecretKey() {
        // Setup
        when(mockEnv.getProperty("osis.scality.vault.secret-key")).thenReturn(TEST_RESULT);

        // Run the test
        final String result = appEnvUnderTest.getPlatformSecretKey();

        // Verify the results
        assertEquals(result, TEST_RESULT, "testGetVaultSecretKey failed");
    }

    @Test
    void testGetVaultSecretKeyEnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("osis.scality.vault.secret-key")).thenReturn(null);

        // Run the test
        final String result = appEnvUnderTest.getPlatformSecretKey();

        // Verify the results
        assertNull(result, TEST_RESULT_NULL_ERR);
    }

    @Test
    void testGetS3InterfaceEndpoint() {
        // Setup
        when(mockEnv.getProperty("osis.scality.vaultS3Interface.endpoint")).thenReturn(TEST_RESULT);

        // Run the test
        final String result = appEnvUnderTest.getS3InterfaceEndpoint();

        // Verify the results
        assertEquals(result, TEST_RESULT, "testGetS3InterfaceEndpoint failed");
    }

    @Test
    void testGetS3InterfaceEndpointEnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("osis.scality.vaultS3Interface.endpoint")).thenReturn(null);

        // Run the test
        final String result = appEnvUnderTest.getS3InterfaceEndpoint();

        // Verify the results
        assertNull(result, TEST_RESULT_NULL_ERR);
    }

    @Test
    void testGetConsoleEndpoint() {
        // Setup
        when(mockEnv.getProperty("osis.scality.console.endpoint")).thenReturn(TEST_RESULT);

        // Run the test
        final String result = appEnvUnderTest.getConsoleEndpoint();

        // Verify the results
        assertEquals(result, TEST_RESULT, "testGetConsoleEndpoint failed");
    }

    @Test
    void testGetConsoleEndpointEnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("osis.scality.console.endpoint")).thenReturn(null);

        // Run the test
        final String result = appEnvUnderTest.getConsoleEndpoint();

        // Verify the results
        assertNull(result, TEST_RESULT_NULL_ERR);
    }

    @Test
    void testGetStorageInfo() {
        // Setup
        when(mockEnv.getProperty("osis.scality.storage-classes")).thenReturn(TEST_RESULT);

        // Run the test
        final List<String> result = appEnvUnderTest.getStorageInfo();

        // Verify the results
        assertEquals(result, Arrays.asList(TEST_RESULT), "testGetStorageInfo failed");
    }

    @Test
    void testGetStorageInfoEnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("osis.scality.storage-classes")).thenReturn(null);

        // Run the test
        final List<String> result = appEnvUnderTest.getStorageInfo();

        // Verify the results
        assertEquals(result, Arrays.asList("standard"), "testGetStorageInfoEnvironmentReturnsNull failed");
    }

    @Test
    void testGetRegionInfo() {
        // Setup
        when(mockEnv.getProperty("osis.scality.region")).thenReturn("region1,region2");

        // Run the test
        final List<String> result = appEnvUnderTest.getRegionInfo();

        // Verify the results
        assertEquals(result, Arrays.asList("region1","region2"), "testGetRegionInfo failed");
    }

    @Test
    void testGetRegionInfoEnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("osis.scality.region")).thenReturn(null);

        // Run the test
        final List<String> result = appEnvUnderTest.getRegionInfo();

        // Verify the results
        assertEquals(result, Arrays.asList("us-east-1"), "testGetRegionInfoEnvironmentReturnsNull failed");
    }

    @Test
    void testGetPlatformName() {
        // Setup
        when(mockEnv.getProperty("osis.scality.name")).thenReturn("scality");

        // Run the test
        final String result = appEnvUnderTest.getPlatformName();

        // Verify the results
        assertEquals(result, "scality", "testGetPlatformName failed");
    }

    @Test
    void testGetPlatformNameEnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("osis.scality.name")).thenReturn(null);

        // Run the test
        final String result = appEnvUnderTest.getPlatformName();

        // Verify the results
        assertNull(result, TEST_RESULT_NULL_ERR);
    }

    @Test
    void testGetPlatformVersion() {
        // Setup
        when(mockEnv.getProperty("osis.scality.version")).thenReturn("v1");

        // Run the test
        final String result = appEnvUnderTest.getPlatformVersion();

        // Verify the results
        assertEquals(result, "v1", "testGetPlatformVersion failed");
    }

    @Test
    void testGetPlatformVersionEnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("osis.scality.version")).thenReturn(null);

        // Run the test
        final String result = appEnvUnderTest.getPlatformVersion();

        // Verify the results
        assertNull(result, TEST_RESULT_NULL_ERR);
    }

    @Test
    void testGetApiVersion() {
        // Setup
        when(mockEnv.getProperty("osis.api.version")).thenReturn("v1");

        // Run the test
        final String result = appEnvUnderTest.getApiVersion();

        // Verify the results
        assertEquals(result, "v1", "testGetApiVersion failed");
    }

    @Test
    void testGetApiVersionEnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("osis.api.version")).thenReturn(null);

        // Run the test
        final String result = appEnvUnderTest.getApiVersion();

        // Verify the results
        assertNull(result, TEST_RESULT_NULL_ERR);
    }

    @Test
    void testIsApiTokenEnabled() {
        // Setup
        when(mockEnv.getProperty("security.jwt.enabled")).thenReturn("true");

        // Run the test
        final boolean result = appEnvUnderTest.isApiTokenEnabled();

        // Verify the results
        assertTrue(result, "testIsApiTokenEnabled failed");
    }

    @Test
    void testIsApiTokenEnabledEnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("security.jwt.enabled")).thenReturn(null);

        // Run the test
        final boolean result = appEnvUnderTest.isApiTokenEnabled();

        // Verify the results
        assertFalse(result, "testIsApiTokenEnabledEnvironmentReturnsNull failed");
    }

    @Test
    void testGetTokenIssuer() {
        // Setup
        when(mockEnv.getProperty("security.jwt.token-issuer")).thenReturn(TEST_RESULT);

        // Run the test
        final String result = appEnvUnderTest.getTokenIssuer();

        // Verify the results
        assertEquals(result, TEST_RESULT, "testGetTokenIssuer failed");
    }

    @Test
    void testGetTokenIssuerEnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("security.jwt.token-issuer")).thenReturn(null);

        // Run the test
        final String result = appEnvUnderTest.getTokenIssuer();

        // Verify the results
        assertNull(result, TEST_RESULT_NULL_ERR);
    }

    @Test
    void testGetAccessTokenExpirationTime() {
        // Setup
        when(mockEnv.getProperty("security.jwt.access-token-expiration-time")).thenReturn("1000");

        // Run the test
        final int result = appEnvUnderTest.getAccessTokenExpirationTime();

        // Verify the results
        assertEquals(result, 1000, "testGetAccessTokenExpirationTime failed");
    }

    @Test
    void testGetAccessTokenExpirationTimeEnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("security.jwt.access-token-expiration-time")).thenReturn(null);

        // Run the test
        // Verify the results
        assertThrows(NumberFormatException.class, ()->appEnvUnderTest.getAccessTokenExpirationTime(), "testGetAccessTokenExpirationTimeEnvironmentReturnsNull should throw NumberFormatException");
    }

    @Test
    void testGetTokenSigningKey() {
        // Setup
        when(mockEnv.getProperty("security.jwt.token-signing-key")).thenReturn(TEST_RESULT);

        // Run the test
        final String result = appEnvUnderTest.getTokenSigningKey();

        // Verify the results
        assertEquals(result, TEST_RESULT, "testGetTokenSigningKey failed");
    }

    @Test
    void testGetTokenSigningKeyEnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("security.jwt.token-signing-key")).thenReturn(null);

        // Run the test
        final String result = appEnvUnderTest.getTokenSigningKey();

        // Verify the results
        assertNull(result, TEST_RESULT_NULL_ERR);
    }

    @Test
    void testGetRefreshTokenExpirationTime() {
        // Setup
        when(mockEnv.getProperty("security.jwt.refresh_token_expiration_time")).thenReturn("1000");

        // Run the test
        final int result = appEnvUnderTest.getRefreshTokenExpirationTime();

        // Verify the results
        assertEquals(result, 1000, "testGetRefreshTokenExpirationTime failed");
    }

    @Test
    void testGetRefreshTokenExpirationTimeEnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("security.jwt.refresh_token_expiration_time")).thenReturn(null);

        // Run the test
        // Verify the results
        assertThrows(NumberFormatException.class, ()->appEnvUnderTest.getRefreshTokenExpirationTime(), "testGetRefreshTokenExpirationTimeEnvironmentReturnsNull should throw NumberFormatException");
    }

    @Test
    void testGetAssumeRoleName() {
        // Setup
        when(mockEnv.getProperty("osis.scality.vault.role.name")).thenReturn("osis");

        // Run the test
        final String result = appEnvUnderTest.getAssumeRoleName();

        // Verify the results
        assertEquals(result, "osis", "testGetAssumeRoleName failed");
    }

    @Test
    void testGetAccountAKDurationSeconds() {
        // Setup
        when(mockEnv.getProperty("osis.scality.vault.account.accessKey.durationSeconds")).thenReturn("120");

        // Run the test
        final long result = appEnvUnderTest.getAccountAKDurationSeconds();

        // Verify the results
        assertThat(result).isEqualTo(120L);
    }

    @Test
    void testGetAccountAKDurationSecondsEnvironmentReturnsNull() {
        // Setup

        // Run the test
        final long result = appEnvUnderTest.getAccountAKDurationSeconds();

        // Verify the results
        assertThat(result).isEqualTo(120L);
    }

    @Test
    void testGetAsyncExecutorCorePoolSize() {
        // Setup
        when(mockEnv.getProperty("osis.scality.async.corePoolSize")).thenReturn("10");

        // Run the test
        final int result = appEnvUnderTest.getAsyncExecutorCorePoolSize();

        // Verify the results
        assertThat(result).isEqualTo(10);
    }

    @Test
    void testGetAsyncExecutorCorePoolSizeEnvironmentReturnsNull() {
        // Setup

        // Run the test
        final int result = appEnvUnderTest.getAsyncExecutorCorePoolSize();

        // Verify the results
        assertThat(result).isEqualTo(10);
    }

    @Test
    void testGetAsyncExecutorMaxPoolSize() {
        // Setup
        when(mockEnv.getProperty("osis.scality.async.maxPoolSize")).thenReturn("10");

        // Run the test
        final int result = appEnvUnderTest.getAsyncExecutorMaxPoolSize();

        // Verify the results
        assertThat(result).isEqualTo(10);
    }

    @Test
    void testGetAsyncExecutorMaxPoolSizeEnvironmentReturnsNull() {
        // Setup

        // Run the test
        final int result = appEnvUnderTest.getAsyncExecutorMaxPoolSize();

        // Verify the results
        assertThat(result).isEqualTo(10);
    }

    @Test
    void testGetAsyncExecutorQueueCapacity() {
        // Setup
        when(mockEnv.getProperty("osis.scality.async.queueCapacity")).thenReturn("500");

        // Run the test
        final int result = appEnvUnderTest.getAsyncExecutorQueueCapacity();

        // Verify the results
        assertThat(result).isEqualTo(500);
    }

    @Test
    void testGetAsyncExecutorQueueCapacityEnvironmentReturnsNull() {
        // Setup

        // Run the test
        final int result = appEnvUnderTest.getAsyncExecutorQueueCapacity();

        // Verify the results
        assertThat(result).isEqualTo(500);
    }

    @Test
    void testGetS3Endpoint() {
        // Setup
        when(mockEnv.getProperty("osis.scality.s3.endpoint")).thenReturn(TEST_RESULT);

        // Run the test
        final String result = appEnvUnderTest.getS3Endpoint();

        // Verify the results
        assertEquals(result, TEST_RESULT, "testGetS3Endpoint failed");
    }

    @Test
    void testGetS3EndpointEnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("osis.scality.s3.endpoint")).thenReturn(null);

        // Run the test
        final String result = appEnvUnderTest.getS3Endpoint();

        // Verify the results
        assertNull(result, TEST_RESULT_NULL_ERR);
    }

    @Test
    void testGetS3HealthCheckTimeout() {
        //Setup
        when(mockEnv.getProperty("osis.scality.s3.healthcheck.timeout")).thenReturn("3000");

        // Run the test
        final Integer result = appEnvUnderTest.getS3HealthCheckTimeout();

        // Verify the results
        assertEquals(result, 3000, "testGetS3HealthCheckTimeout failed");
    }

    @Test
    void testIsUtapiEnabled() {
        // Setup
        when(mockEnv.getProperty("osis.scality.utapi.enabled")).thenReturn("true");

        // Run the test
        final Boolean result = appEnvUnderTest.isUtapiEnabled();

        // Verify the results
        assertEquals(result, true, "testIsUtapiEnabled failed");
    }

    @Test
    void testGetUtapiEndpoint() {
        // Setup
        when(mockEnv.getProperty("osis.scality.utapi.endpoint")).thenReturn(TEST_RESULT);

        // Run the test
        final String result = appEnvUnderTest.getUtapiEndpoint();

        // Verify the results
        assertEquals(result, TEST_RESULT, "testGetUtapiEndpoint failed");
    }

    @Test
    void testGetUtapiEndpointEnvironmentReturnsNull() {
        // Setup
        when(mockEnv.getProperty("osis.scality.utapi.endpoint")).thenReturn(null);

        // Run the test
        final String result = appEnvUnderTest.getUtapiEndpoint();

        // Verify the results
        assertNull(result, TEST_RESULT_NULL_ERR);
    }

    @Test
    void testGetUtapiHealthCheckTimeout() {
        //Setup
        when(mockEnv.getProperty("osis.scality.utapi.healthcheck.timeout")).thenReturn("3000");

        // Run the test
        final Integer result = appEnvUnderTest.getUtapiHealthCheckTimeout();

        // Verify the results
        assertEquals(result, 3000, "testGetUtapiHealthCheckTimeout failed");
    }
}
