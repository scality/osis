package com.scality.osis.service.tenant;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.AmazonIdentityManagementException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.scality.osis.ScalityAppEnv;
import com.scality.osis.s3.impl.S3Impl;
import com.scality.osis.service.impl.AsyncScalityOsisService;
import com.scality.osis.utapiclient.utils.UtapiClientException;
import com.scality.osis.vaultadmin.impl.VaultAdminImpl;
import com.scality.osis.vaultadmin.impl.VaultServiceException;
import com.scality.vaultclient.dto.AccountData;
import com.scality.vaultclient.dto.GetAccountRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.Date;

import static com.scality.osis.utils.ScalityConstants.ACCESS_DENIED;
import static com.scality.osis.utils.ScalityTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"PMD.ExcessiveImports", "PMD.TooManyStaticImports",
        "PMD.SignatureDeclareThrowsException"})
class ScalityTenantSessionTest {

    @Mock
    private ScalityAppEnv appEnvMock;

    @Mock
    private VaultAdminImpl vaultAdminMock;

    @Mock
    private S3Impl s3Mock;

    @Mock
    private AsyncScalityOsisService asyncScalityOsisServiceMock;

    @Mock
    private AmazonIdentityManagement iamMock;

    @Mock
    private AmazonS3 s3ClientMock;

    private ScalityTenantSession tenantSessionUnderTest;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);

        when(appEnvMock.getRegionInfo()).thenReturn(Collections.singletonList("default"));
        when(appEnvMock.getAssumeRoleName()).thenReturn(SAMPLE_ASSUME_ROLE_NAME);

        when(vaultAdminMock.getTempAccountCredentials(any(AssumeRoleRequest.class)))
                .thenAnswer((Answer<Credentials>) invocation -> sampleCredentials());
        when(vaultAdminMock.getIAMClient(any(Credentials.class), any())).thenReturn(iamMock);
        when(s3Mock.getS3Client(any(Credentials.class), any())).thenReturn(s3ClientMock);
        when(vaultAdminMock.getAccount(any(GetAccountRequestDTO.class)))
                .thenAnswer((Answer<AccountData>) invocation -> {
                    final AccountData data = new AccountData();
                    data.setId(TEST_TENANT_ID);
                    data.setName(SAMPLE_TENANT_NAME);
                    return data;
                });

        tenantSessionUnderTest = new ScalityTenantSession(appEnvMock, vaultAdminMock, s3Mock,
                asyncScalityOsisServiceMock);
    }

    private Credentials sampleCredentials() {
        final Credentials credentials = new Credentials();
        credentials.setAccessKeyId(TEST_ACCESS_KEY);
        credentials.setSecretAccessKey(TEST_SECRET_KEY);
        credentials.setExpiration(new Date());
        credentials.setSessionToken(TEST_SESSION_TOKEN);
        return credentials;
    }

    private AmazonIdentityManagementException forbiddenIamError() {
        final AmazonIdentityManagementException error = new AmazonIdentityManagementException("Forbidden");
        error.setStatusCode(HttpStatus.FORBIDDEN.value());
        return error;
    }

    @Test
    void buildsIamClientWithAssumedRoleCredentials() {
        final AmazonIdentityManagement iam = tenantSessionUnderTest.getIamClient(TEST_TENANT_ID);

        assertSame(iamMock, iam);
        verify(vaultAdminMock).getTempAccountCredentials(any(AssumeRoleRequest.class));
        verify(vaultAdminMock).getIAMClient(any(Credentials.class), any());
    }

    @Test
    void buildsS3ClientWithAssumedRoleCredentials() {
        final AmazonS3 s3Client = tenantSessionUnderTest.getS3Client(TEST_TENANT_ID);

        assertSame(s3ClientMock, s3Client);
        verify(vaultAdminMock).getTempAccountCredentials(any(AssumeRoleRequest.class));
        verify(s3Mock).getS3Client(any(Credentials.class), any());
    }

    @Test
    void recreatesRoleOnAccessDeniedThenRetries() {
        when(vaultAdminMock.getTempAccountCredentials(any(AssumeRoleRequest.class)))
                .thenThrow(new VaultServiceException(HttpStatus.FORBIDDEN, "AccessDenied",
                        "User: backbeat is not allowed to assume role"))
                .thenAnswer((Answer<Credentials>) invocation -> sampleCredentials());

        final AmazonIdentityManagement iam = tenantSessionUnderTest.getIamClient(TEST_TENANT_ID);

        assertSame(iamMock, iam);
        // setupAssumeRole called once with the account name resolved via getAccount
        verify(asyncScalityOsisServiceMock).setupAssumeRole(TEST_TENANT_ID, SAMPLE_TENANT_NAME);
        verify(vaultAdminMock, times(2)).getTempAccountCredentials(any(AssumeRoleRequest.class));
    }

    @Test
    void stopsRetryingWhenAccessDeniedPersistsAfterRecreatingRole() {
        when(vaultAdminMock.getTempAccountCredentials(any(AssumeRoleRequest.class)))
                .thenThrow(new VaultServiceException(HttpStatus.FORBIDDEN, "AccessDenied",
                        "User: backbeat is not allowed to assume role"));

        // persistent access-denied must surface as a VaultServiceException, not recurse forever
        final VaultServiceException error = assertThrows(VaultServiceException.class,
                () -> tenantSessionUnderTest.getIamClient(TEST_TENANT_ID));
        assertEquals(ACCESS_DENIED, error.getErrorCode());

        // recovery routine invoked at most once
        verify(asyncScalityOsisServiceMock, times(1)).setupAssumeRole(TEST_TENANT_ID, SAMPLE_TENANT_NAME);
        // exactly one recovery means one original attempt plus one retry
        verify(vaultAdminMock, times(2)).getTempAccountCredentials(any(AssumeRoleRequest.class));
        verifyNoInteractions(iamMock);
    }

    @Test
    void rethrowsNonAccessDeniedVaultError() {
        when(vaultAdminMock.getTempAccountCredentials(any(AssumeRoleRequest.class)))
                .thenThrow(new VaultServiceException(HttpStatus.BAD_REQUEST, "Bad Request"));

        assertThrows(VaultServiceException.class, () -> tenantSessionUnderTest.getIamClient(TEST_TENANT_ID));
        verify(asyncScalityOsisServiceMock, never()).setupAssumeRole(anyString(), anyString());
    }

    @Test
    void runReturnsOperationResultOnSuccess() throws Exception {
        final String result = tenantSessionUnderTest.run(TEST_TENANT_ID, () -> "ok");

        assertEquals("ok", result);
        verify(asyncScalityOsisServiceMock, never()).setupAdminPolicy(anyString(), anyString(), anyString());
    }

    @Test
    void runGeneratesAdminPolicyAndRetriesOnceOnAdminPolicyError() throws Exception {
        final boolean[] failedOnce = {false};

        final String result = tenantSessionUnderTest.run(TEST_TENANT_ID, () -> {
            if (!failedOnce[0]) {
                failedOnce[0] = true;
                throw forbiddenIamError();
            }
            return "ok";
        });

        assertEquals("ok", result);
        verify(asyncScalityOsisServiceMock).setupAdminPolicy(TEST_TENANT_ID, SAMPLE_TENANT_NAME, SAMPLE_ASSUME_ROLE_NAME);
    }

    @Test
    void runRethrowsWhenAdminPolicyErrorRepeatsAfterRetry() throws Exception {
        assertThrows(AmazonIdentityManagementException.class,
                () -> tenantSessionUnderTest.run(TEST_TENANT_ID, () -> {
                    throw forbiddenIamError();
                }));

        // recovery attempted exactly once
        verify(asyncScalityOsisServiceMock, times(1))
                .setupAdminPolicy(TEST_TENANT_ID, SAMPLE_TENANT_NAME, SAMPLE_ASSUME_ROLE_NAME);
    }

    @Test
    void runDoesNotRetryOnNonAdminPolicyError() throws Exception {
        assertThrows(IllegalStateException.class,
                () -> tenantSessionUnderTest.run(TEST_TENANT_ID, () -> {
                    throw new IllegalStateException("boom");
                }));

        verify(asyncScalityOsisServiceMock, never()).setupAdminPolicy(anyString(), anyString(), anyString());
    }

    @Test
    void runDoesNotRetryWhenTenantIdIsEmpty() throws Exception {
        assertThrows(AmazonIdentityManagementException.class,
                () -> tenantSessionUnderTest.run("", () -> {
                    throw forbiddenIamError();
                }));

        verify(asyncScalityOsisServiceMock, never()).setupAdminPolicy(anyString(), anyString(), anyString());
    }

    @Test
    void runTreatsForbiddenS3ErrorAsAdminPolicyError() throws Exception {
        final boolean[] failedOnce = {false};

        final String result = tenantSessionUnderTest.run(TEST_TENANT_ID, () -> {
            if (!failedOnce[0]) {
                failedOnce[0] = true;
                final AmazonS3Exception error = new AmazonS3Exception("Forbidden");
                error.setStatusCode(HttpStatus.FORBIDDEN.value());
                throw error;
            }
            return "ok";
        });

        assertEquals("ok", result);
        verify(asyncScalityOsisServiceMock).setupAdminPolicy(TEST_TENANT_ID, SAMPLE_TENANT_NAME, SAMPLE_ASSUME_ROLE_NAME);
    }

    @Test
    void runTreatsForbiddenUtapiErrorAsAdminPolicyError() throws Exception {
        final boolean[] failedOnce = {false};

        final String result = tenantSessionUnderTest.run(TEST_TENANT_ID, () -> {
            if (!failedOnce[0]) {
                failedOnce[0] = true;
                final UtapiClientException error = new UtapiClientException("Forbidden");
                error.setStatusCode(HttpStatus.FORBIDDEN.value());
                throw error;
            }
            return "ok";
        });

        assertEquals("ok", result);
        verify(asyncScalityOsisServiceMock).setupAdminPolicy(TEST_TENANT_ID, SAMPLE_TENANT_NAME, SAMPLE_ASSUME_ROLE_NAME);
    }
}
