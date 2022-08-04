package com.scality.osis.service.impl;

import com.amazonaws.services.identitymanagement.model.*;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.scality.osis.model.*;
import com.scality.osis.model.exception.NotImplementedException;
import com.scality.osis.s3.impl.S3ServiceException;
import com.scality.osis.vaultadmin.impl.VaultServiceException;
import com.scality.vaultclient.dto.GenerateAccountAccessKeyRequest;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpStatus;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.scality.osis.utils.ScalityConstants.IAM_PREFIX;
import static com.scality.osis.utils.ScalityTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ScalityOsisServiceMiscTests extends BaseOsisServiceTest {

    @Test
    public void testGetProviderConsoleUrl() {
        // Setup
        final String consoleUrl = scalityOsisServiceUnderTest.getProviderConsoleUrl();
        // Run the test
        assertNotNull(consoleUrl, NULL_ERR);
        assertEquals(TEST_CONSOLE_URL, consoleUrl, INVALID_URL_URL);

        // Verify the results
    }

    @Test
    public void testGetTenantConsoleUrl() {
        // Setup
        final String consoleUrl = scalityOsisServiceUnderTest.getTenantConsoleUrl(TEST_TENANT_ID);
        // Run the test
        assertNotNull(consoleUrl, NULL_ERR);
        assertEquals(TEST_CONSOLE_URL, consoleUrl, INVALID_URL_URL);

        // Verify the results
    }

    @Test
    public void testGetS3Capabilities() {
        // Setup
        // Run the test
        assertNotNull(scalityOsisServiceUnderTest.getS3Capabilities(), NULL_ERR);

        // Verify the results
    }

    @Test
    public void testGetInformationWithBasicAuth() {
        // Setup
        final String domain = "https://localhost:8443";

        // Run the test
        final Information information = scalityOsisServiceUnderTest.getInformation(domain);

        // Verify the results
        assertEquals(Information.AuthModesEnum.BASIC, information.getAuthModes().get(0), "Invalid AuthModes");
        assertNotNull(information.getStorageClasses(), NULL_ERR);
        assertNotNull(information.getRegions(), NULL_ERR);
        assertEquals(PLATFORM_NAME, information.getPlatformName(), "Invalid Platform name");
        assertEquals(PLATFORM_VERSION, information.getPlatformVersion(), "Invalid Platform Version");
        assertEquals(API_VERSION, information.getApiVersion(), "Invalid API Version");
        assertEquals(Information.StatusEnum.NORMAL, information.getStatus(), "Invalid status");
        assertEquals(TEST_S3_URL, information.getServices().getS3(), "Invalid S3 URL");
        assertNotNull(information.getNotImplemented(), NULL_ERR);
        assertEquals(domain + IAM_PREFIX, information.getServices().getIam(), "Invalid IAM URL");

    }

    @Test
    public void testGetInformationWithBearerAuth() {
        // Setup
        final String domain = "https://localhost:8443";
        when(appEnvMock.isApiTokenEnabled()).thenReturn(true);

        // Run the test
        final Information information = scalityOsisServiceUnderTest.getInformation(domain);

        // Verify the results
        assertEquals(Information.AuthModesEnum.BEARER, information.getAuthModes().get(0), "Invalid AuthModes");
        assertNotNull(information.getStorageClasses(), NULL_ERR);
        assertNotNull(information.getRegions(), NULL_ERR);
        assertEquals(PLATFORM_NAME, information.getPlatformName(), "Invalid Platform name");
        assertEquals(PLATFORM_VERSION, information.getPlatformVersion(), "Invalid Platform Version");
        assertEquals(API_VERSION, information.getApiVersion(), "Invalid API Version");
        assertEquals(Information.StatusEnum.NORMAL, information.getStatus(), "Invalid status");
        assertEquals(TEST_S3_URL, information.getServices().getS3(), "Invalid S3 URL");
        assertNotNull(information.getNotImplemented(), NULL_ERR);
        assertEquals(domain + IAM_PREFIX, information.getServices().getIam(), "Invalid IAM URL");
    }

    @Test
    public void testUpdateOsisCaps() {
        // Setup
        final ScalityOsisCaps osisCaps = new ScalityOsisCaps();

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.updateOsisCaps(osisCaps),
                NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

    @Test
    public void testGetBucketList() {
        // Setup
        final long offset = 0L;
        final long limit = 1000L;

        // Run the test
        final PageOfOsisBucketMeta response = scalityOsisServiceUnderTest.getBucketList(SAMPLE_TENANT_ID, offset, limit);

        // Verify the results
        assertEquals(TEST_BUCKET_TOTAL_NUMBER, response.getPageInfo().getTotal());
        assertEquals(offset, response.getPageInfo().getOffset());
        assertEquals(limit, response.getPageInfo().getLimit());
        assertEquals((int) limit, response.getItems().size());
    }

    @Test
    public void testGetBucketListWithOffset() {
        // Setup
        final long offset = 2000L;
        final long limit = 1000L;

        // Run the test
        final PageOfOsisBucketMeta response = scalityOsisServiceUnderTest.getBucketList(SAMPLE_TENANT_ID, offset, limit);

        // Verify the results
        assertEquals(TEST_BUCKET_TOTAL_NUMBER, response.getPageInfo().getTotal());
        assertEquals(offset, response.getPageInfo().getOffset());
        assertEquals(limit, response.getPageInfo().getLimit());
        assertEquals((int) limit, response.getItems().size());
    }

    @Test
    public void testGetBucketListErr() {
        // Setup
        final long offset = 0L;
        final long limit = 1000L;
        when(s3ClientMock.listBuckets())
                .thenAnswer((Answer<List<Bucket>>) invocation -> {
                    throw new S3ServiceException(HttpStatus.BAD_REQUEST,
                            "Requested offset is outside the total available items");
                });

        final PageOfOsisBucketMeta response = scalityOsisServiceUnderTest.getBucketList(SAMPLE_TENANT_ID, offset, limit);

        // Verify the results
        assertEquals(0L, response.getPageInfo().getTotal());
        assertEquals(offset, response.getPageInfo().getOffset());
        assertEquals(limit, response.getPageInfo().getLimit());
        assertEquals(0L, response.getItems().size());
    }

    @Test
    public void testGetOsisUsage() {
        // Setup

        // Run the test
        assertNotNull(scalityOsisServiceUnderTest.getOsisUsage(Optional.of(TEST_STR), Optional.of(TEST_STR)), NULL_ERR);

        // Verify the results

    }

    @Test
    public void testGetCredentials() {
        // Setup

        // Run the test
        final Credentials credentials = scalityOsisServiceUnderTest.getCredentials(TEST_TENANT_ID);

        // Verify the results
        assertEquals(TEST_ACCESS_KEY, credentials.getAccessKeyId(), "Invalid Access key");
        assertEquals(TEST_SECRET_KEY, credentials.getSecretAccessKey(), "Invalid Secret Key");
    }

    @Test
    public void testGetCredentialsWithNoRole() {
        // Setup

        when(vaultAdminMock.getTempAccountCredentials(any(AssumeRoleRequest.class)))
                .thenThrow(new VaultServiceException(HttpStatus.NOT_FOUND, "NoSuchEntity", "Role does not exist"))
                .thenAnswer((Answer<Credentials>) invocation -> {
                    final Credentials credentials = new Credentials();
                    credentials.setAccessKeyId(TEST_ACCESS_KEY);
                    credentials.setSecretAccessKey(TEST_SECRET_KEY);
                    credentials.setExpiration(new Date());
                    credentials.setSessionToken(TEST_SESSION_TOKEN);

                    return credentials;
                });
        // Run the test
        final Credentials credentials = scalityOsisServiceUnderTest.getCredentials(TEST_TENANT_ID);

        // Verify the results
        assertEquals(TEST_ACCESS_KEY, credentials.getAccessKeyId(), "Invalid Access key");
        assertEquals(TEST_SECRET_KEY, credentials.getSecretAccessKey(), "Invalid Secret Key");
    }

    @Test
    public void testGetCredentials400() {
        // Setup
        when(vaultAdminMock.getTempAccountCredentials(any(AssumeRoleRequest.class)))
                .thenThrow(new VaultServiceException(HttpStatus.BAD_REQUEST, "Bad Request"));

        // Run the test
        assertThrows(VaultServiceException.class, () -> scalityOsisServiceUnderTest.getCredentials(TEST_TENANT_ID));

        // Verify the results
    }

    @Test
    public void testSetupAssumeRoleAsync() {

        asyncScalityOsisServiceUnderTest.setupAssumeRole(createSampleOsisTenantObj());

        // Verify if all the API calls were made successfully
        verify(vaultAdminMock).getAccountAccessKey(any(GenerateAccountAccessKeyRequest.class));
        verify(iamMock).createRole(any(CreateRoleRequest.class));
        verify(iamMock).createPolicy(any(CreatePolicyRequest.class));
        verify(iamMock).attachRolePolicy(any(AttachRolePolicyRequest.class));
        verify(iamMock).deleteAccessKey(any(DeleteAccessKeyRequest.class));
    }

    @Test
    public void testSetupAssumeRole() {

        asyncScalityOsisServiceUnderTest.setupAssumeRole(SAMPLE_TENANT_ID, SAMPLE_TENANT_NAME);

        // Verify if all the API calls were made successfully
        verify(vaultAdminMock).getAccountAccessKey(any(GenerateAccountAccessKeyRequest.class));
        verify(iamMock).createRole(any(CreateRoleRequest.class));
        verify(iamMock).createPolicy(any(CreatePolicyRequest.class));
        verify(iamMock).attachRolePolicy(any(AttachRolePolicyRequest.class));
        verify(iamMock).deleteAccessKey(any(DeleteAccessKeyRequest.class));
    }

    @Test
    public void testSetupAssumeRoleCreateRoleFail() {
        when(iamMock.createRole(any(CreateRoleRequest.class)))
                .thenAnswer((Answer<CreateRoleResult>) invocation -> {
                    throw new VaultServiceException(HttpStatus.NOT_FOUND, "MalformedPolicyDocument");
                });

        asyncScalityOsisServiceUnderTest.setupAssumeRole(SAMPLE_TENANT_ID, SAMPLE_TENANT_NAME);

        // Verify if all the API calls were skipped after create role if it returns
        // "NoSuchEntity"
        verify(vaultAdminMock).getAccountAccessKey(any(GenerateAccountAccessKeyRequest.class));
        verify(iamMock).createRole(any(CreateRoleRequest.class));
        verify(iamMock, never()).createPolicy(any(CreatePolicyRequest.class));
        verify(iamMock, never()).attachRolePolicy(any(AttachRolePolicyRequest.class));
        verify(iamMock, never()).deleteAccessKey(any(DeleteAccessKeyRequest.class));
    }

    @Test
    public void testSetupAssumeRoleCreatePolicyFail() {
        when(iamMock.createPolicy(any(CreatePolicyRequest.class)))
                .thenAnswer((Answer<CreatePolicyResult>) invocation -> {
                    throw new VaultServiceException(HttpStatus.CONFLICT, "EntityAlreadyExists");
                });

        asyncScalityOsisServiceUnderTest.setupAssumeRole(SAMPLE_TENANT_ID, SAMPLE_TENANT_NAME);

        // Verify if all the API calls were made successfully even after createPolicy
        // returns "EntityAlreadyExists"
        verify(vaultAdminMock).getAccountAccessKey(any(GenerateAccountAccessKeyRequest.class));
        verify(iamMock).createRole(any(CreateRoleRequest.class));
        verify(iamMock).createPolicy(any(CreatePolicyRequest.class));
        verify(iamMock).attachRolePolicy(any(AttachRolePolicyRequest.class));
        verify(iamMock).deleteAccessKey(any(DeleteAccessKeyRequest.class));
    }

    @Test
    public void testSetupAssumeRoleAttachPolicyFail() {
        when(iamMock.attachRolePolicy(any(AttachRolePolicyRequest.class)))
                .thenAnswer((Answer<AttachRolePolicyResult>) invocation -> {
                    throw new VaultServiceException(HttpStatus.NOT_FOUND, "NoSuchEntity");
                });

        asyncScalityOsisServiceUnderTest.setupAssumeRole(SAMPLE_TENANT_ID, SAMPLE_TENANT_NAME);

        // Verify if all the API calls were skipped after attachRolePolicy if it returns
        // "NoSuchEntity"
        verify(vaultAdminMock).getAccountAccessKey(any(GenerateAccountAccessKeyRequest.class));
        verify(iamMock).createRole(any(CreateRoleRequest.class));
        verify(iamMock).createPolicy(any(CreatePolicyRequest.class));
        verify(iamMock).attachRolePolicy(any(AttachRolePolicyRequest.class));
        verify(iamMock, never()).deleteAccessKey(any(DeleteAccessKeyRequest.class));
    }

    @Test
    public void testGetOrCreateUserPolicy1() {
        // Setup
        // Run the test
        final Policy policy = scalityOsisServiceUnderTest.getOrCreateUserPolicy(iamMock, TEST_TENANT_ID);

        // Verify the results
        assertEquals("arn:aws:iam::" + TEST_TENANT_ID + ":policy/userPolicy@" + TEST_TENANT_ID, policy.getArn(),
                "Invalid Policy arn");
    }

    @Test
    public void testGetOrCreateUserPolicy2() {
        // Setup
        // Modify get policy to return no entity found
        when(iamMock.getPolicy(any()))
                .thenAnswer((Answer<GetPolicyResult>) invocation -> {
                    throw new NoSuchEntityException("Entity does not exist");
                });
        // Run the test
        final Policy policy = scalityOsisServiceUnderTest.getOrCreateUserPolicy(iamMock, TEST_TENANT_ID);

        // Verify the results
        assertEquals("arn:aws:iam::" + TEST_TENANT_ID + ":policy/userPolicy@" + TEST_TENANT_ID, policy.getArn(),
                "Invalid Policy arn");
        assertEquals("userPolicy@" + TEST_TENANT_ID, policy.getPolicyName(), "Invalid Policy name");
    }

    @Test
    public void testCreateOsisCredential() throws Exception {
        // Setup
        // Modify get policy to return no entity found

        // Run the test
        final OsisS3Credential osisCredential = scalityOsisServiceUnderTest.createOsisCredential(TEST_TENANT_ID,
                TEST_USER_ID, TEST_TENANT_ID, TEST_NAME, iamMock);

        // Verify the results
        assertEquals(TEST_NAME, osisCredential.getUsername(), "Invalid username");
        assertEquals(TEST_USER_ID, osisCredential.getUserId(), "Invalid getUserId");
        assertEquals(TEST_USER_ID, osisCredential.getCdUserId(), "Invalid getCdUserId");
        assertEquals(TEST_SECRET_KEY, osisCredential.getSecretKey(), "Invalid getSecretKey");
        assertEquals(TEST_ACCESS_KEY, osisCredential.getAccessKey(), "Invalid getAccessKey");
        assertEquals(TEST_TENANT_ID, osisCredential.getTenantId(), "Invalid getTenantId");
        assertEquals(TEST_TENANT_ID, osisCredential.getCdTenantId(), "Invalid getCdTenantId");
    }

    @Test
    public void testSetupAdminPolicy() throws Exception {

        asyncScalityOsisServiceUnderTest.setupAdminPolicy(SAMPLE_TENANT_ID, SAMPLE_TENANT_NAME,
                SAMPLE_ASSUME_ROLE_NAME);

        // Verify if all the API calls were made successfully
        verify(vaultAdminMock).getAccountAccessKey(any(GenerateAccountAccessKeyRequest.class));
        verify(iamMock).createPolicy(any(CreatePolicyRequest.class));
        verify(iamMock).attachRolePolicy(any(AttachRolePolicyRequest.class));
        verify(iamMock).deleteAccessKey(any(DeleteAccessKeyRequest.class));
    }

    @Test
    public void testSetupAdminPolicyCreatePolicyFail() throws Exception {
        when(iamMock.createPolicy(any(CreatePolicyRequest.class)))
                .thenAnswer((Answer<CreatePolicyResult>) invocation -> {
                    throw new VaultServiceException(HttpStatus.CONFLICT, "EntityAlreadyExists");
                });

        asyncScalityOsisServiceUnderTest.setupAdminPolicy(SAMPLE_TENANT_ID, SAMPLE_TENANT_NAME,
                SAMPLE_ASSUME_ROLE_NAME);

        // Verify if all the API calls were made successfully even after createPolicy
        // returns "EntityAlreadyExists"
        verify(vaultAdminMock).getAccountAccessKey(any(GenerateAccountAccessKeyRequest.class));
        verify(iamMock).createPolicy(any(CreatePolicyRequest.class));
        verify(iamMock).attachRolePolicy(any(AttachRolePolicyRequest.class));
        verify(iamMock).deleteAccessKey(any(DeleteAccessKeyRequest.class));
    }

    @Test
    public void testSetupAdminPolicyAttachPolicyFail() {
        when(iamMock.attachRolePolicy(any(AttachRolePolicyRequest.class)))
                .thenAnswer((Answer<AttachRolePolicyResult>) invocation -> {
                    throw new VaultServiceException(HttpStatus.NOT_FOUND, "NoSuchEntity");
                });

        assertThrows(VaultServiceException.class, () -> asyncScalityOsisServiceUnderTest
                .setupAdminPolicy(SAMPLE_TENANT_ID, SAMPLE_TENANT_NAME, SAMPLE_ASSUME_ROLE_NAME));

        // Verify if all the API calls were skipped after attachRolePolicy if it returns
        // "NoSuchEntity"
        verify(vaultAdminMock).getAccountAccessKey(any(GenerateAccountAccessKeyRequest.class));
        verify(iamMock).createPolicy(any(CreatePolicyRequest.class));
        verify(iamMock).attachRolePolicy(any(AttachRolePolicyRequest.class));
        verify(iamMock, never()).deleteAccessKey(any(DeleteAccessKeyRequest.class));
    }

    @Test
    public void testSetupExistingButNotEqualAdminPolicy() throws Exception {
        when(iamMock.createPolicy(any(CreatePolicyRequest.class)))
                .thenAnswer((Answer<CreatePolicyResult>) invocation -> {
                    final AmazonIdentityManagementException error = new AmazonIdentityManagementException("EntityAlreadyExists");
                    error.setStatusCode(HttpStatus.CONFLICT.value());
                    throw error;
                });
        when(iamMock.getPolicyVersion(any(GetPolicyVersionRequest.class)))
                .thenAnswer((Answer<GetPolicyVersionResult>) invocation ->
                        new GetPolicyVersionResult().withPolicyVersion(new PolicyVersion().withDocument("")));

        asyncScalityOsisServiceUnderTest.setupAdminPolicy(SAMPLE_TENANT_ID, SAMPLE_TENANT_NAME,
                SAMPLE_ASSUME_ROLE_NAME);

        // Verify if the following API calls were made successfully when setup
        // existing admin policy but policy documents are not equal
        verify(iamMock).getPolicy(any(GetPolicyRequest.class));
        verify(iamMock).getPolicyVersion(any(GetPolicyVersionRequest.class));
        verify(iamMock).createPolicyVersion(any(CreatePolicyVersionRequest.class));
    }

}
