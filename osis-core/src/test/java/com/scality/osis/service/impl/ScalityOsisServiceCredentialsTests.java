package com.scality.osis.service.impl;

import com.amazonaws.services.identitymanagement.model.*;
import com.scality.osis.model.OsisS3Credential;
import com.scality.osis.model.PageOfS3Credentials;
import com.scality.osis.vaultadmin.impl.VaultServiceException;
import com.scality.vaultclient.dto.GetUserByAccessKeyRequestDTO;
import com.scality.vaultclient.dto.GetUserByAccessKeyResponseDTO;
import com.scality.vaultclient.dto.UserData;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.Date;

import static com.scality.osis.utils.ScalityConstants.*;
import static com.scality.osis.utils.ScalityTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScalityOsisServiceCredentialsTests extends BaseOsisServiceTest {

    @Test
    void testCreateS3Credential() {
        // Setup

        // Run the test
        final OsisS3Credential response = scalityOsisServiceUnderTest.createS3Credential(SAMPLE_TENANT_ID,
                TEST_USER_ID);

        // Verify the results
        assertNotNull(response.getAccessKey());
        assertNotNull(response.getSecretKey());
        assertEquals(TEST_USER_ID, response.getUserId());
        assertEquals(TEST_USER_ID, response.getCdUserId());
        assertEquals(SAMPLE_TENANT_ID, response.getTenantId());
        assertEquals(SAMPLE_CD_TENANT_ID, response.getCdTenantId());
    }

    @Test
    void testCreateS3CredentialErr() {
        // Setup
        when(iamMock.createAccessKey(any(CreateAccessKeyRequest.class)))
                .thenThrow(
                        new NoSuchEntityException(
                                "The request was rejected because it referenced an entity that does not exist. " +
                                        "The error message describes the entity."));

        // Run the test
        // Verify the results
        assertThrows(VaultServiceException.class,
                () -> scalityOsisServiceUnderTest.createS3Credential(SAMPLE_TENANT_ID, TEST_USER_ID));

    }

    @Test
    void testCreateS3CredentialNoAdminPolicy() throws Exception {
        // Setup
        when(iamMock.createAccessKey(any(CreateAccessKeyRequest.class)))
                .thenAnswer((Answer<CreateAccessKeyResult>) invocation -> {
                    final AmazonIdentityManagementException iamException = new AmazonIdentityManagementException(
                            "Forbidden");
                    iamException.setStatusCode(HttpStatus.FORBIDDEN.value());
                    throw iamException;
                })
                .thenAnswer((Answer<CreateAccessKeyResult>) invocation -> createAccessKeyMockResponse(invocation));

        /// Run the test
        final OsisS3Credential response = scalityOsisServiceUnderTest.createS3Credential(SAMPLE_TENANT_ID,
                TEST_USER_ID);

        // Verify the results
        assertNotNull(response.getAccessKey());
        assertNotNull(response.getSecretKey());
        assertEquals(TEST_USER_ID, response.getUserId());
        assertEquals(TEST_USER_ID, response.getCdUserId());
        assertEquals(SAMPLE_TENANT_ID, response.getTenantId());
        assertEquals(SAMPLE_CD_TENANT_ID, response.getCdTenantId());

    }

    @Test
    void testQueryS3Credentials() {
        // Setup
        final String filter = TENANT_ID_PREFIX + TEST_TENANT_ID + FILTER_SEPARATOR + USER_ID_PREFIX + TEST_USER_ID;
        final long offset = 0L;
        final long limit = 1000L;

        // Run the test
        final PageOfS3Credentials pageOfS3Credentials = scalityOsisServiceUnderTest.queryS3Credentials(offset, limit,
                filter);

        // Verify the results
        assertNotNull(pageOfS3Credentials.getItems());
        assertEquals(TEST_TENANT_ID, pageOfS3Credentials.getItems().get(0).getTenantId());
        assertEquals(TEST_USER_ID, pageOfS3Credentials.getItems().get(0).getCdUserId());
        assertEquals(TEST_USER_ID, pageOfS3Credentials.getItems().get(0).getUserId());
        assertTrue(pageOfS3Credentials.getPageInfo().getTotal() > 0);
        assertEquals(offset, pageOfS3Credentials.getPageInfo().getOffset());
        assertEquals(limit, pageOfS3Credentials.getPageInfo().getLimit());
        assertFalse(pageOfS3Credentials.getItems().isEmpty());
    }

    @Test
    void testQueryS3CredentialsAccessKeyFilter() {
        // Setup
        final String filter = TENANT_ID_PREFIX + TEST_TENANT_ID
                + FILTER_SEPARATOR
                + USER_ID_PREFIX + TEST_USER_ID
                + FILTER_SEPARATOR
                + OSIS_ACCESS_KEY + FILTER_KEY_VALUE_SEPARATOR + TEST_ACCESS_KEY;
        final long offset = 0L;
        final long limit = 1000L;

        // Run the test
        final PageOfS3Credentials pageOfS3Credentials = scalityOsisServiceUnderTest.queryS3Credentials(offset, limit,
                filter);

        // Verify the results
        assertEquals(1, pageOfS3Credentials.getItems().size());
        assertEquals(1, pageOfS3Credentials.getPageInfo().getTotal());
        assertEquals(offset, pageOfS3Credentials.getPageInfo().getOffset());
        assertEquals(limit, pageOfS3Credentials.getPageInfo().getLimit());

        final OsisS3Credential result = pageOfS3Credentials.getItems().get(0);

        assertEquals(TEST_USER_ID, result.getCdUserId());
        assertEquals(TEST_USER_ID, result.getUserId());
        assertEquals(TEST_TENANT_ID, result.getTenantId());
        assertEquals(TEST_ACCESS_KEY, result.getAccessKey());
        assertEquals(TEST_SECRET_KEY, result.getSecretKey());
        assertTrue(result.getActive());
        assertNotNull(result.getCreationDate());
    }

    @Test
    void testQueryS3CredentialsInvalidFilter() {
        // Setup
        final long offset = 0L;
        final long limit = 1000L;

        // Run the test
        final PageOfS3Credentials response = scalityOsisServiceUnderTest.queryS3Credentials(offset, limit, "");

        // Verify the results
        assertEquals(0L, response.getPageInfo().getTotal());
        assertEquals(offset, response.getPageInfo().getOffset());
        assertEquals(limit, response.getPageInfo().getLimit());
        assertEquals(0L, response.getItems().size());

    }

    @Test
    void testDeleteS3Credential() {
        // Setup

        // Run the test
        scalityOsisServiceUnderTest.deleteS3Credential(TEST_TENANT_ID, TEST_USER_ID, TEST_ACCESS_KEY);

        // Verify the results
        verify(iamMock).deleteAccessKey(any(DeleteAccessKeyRequest.class));
        verify(redisRepositoryMock).delete(any());
    }

    @Test
    void testDeleteS3CredentialWithNoKeyOnRedis() {
        // Setup
        when(redisRepositoryMock.hasKey(any())).thenReturn(Boolean.FALSE);

        // Run the test
        scalityOsisServiceUnderTest.deleteS3Credential(TEST_TENANT_ID, TEST_USER_ID, TEST_ACCESS_KEY);

        // Verify the results
        verify(iamMock).deleteAccessKey(any(DeleteAccessKeyRequest.class));
        verify(redisRepositoryMock, never()).delete(any());
    }

    @Test
    void testDeleteS3CredentialWithoutTenantIdAndUserId() {
        // set up
        when(vaultAdminMock.getUserByAccessKey(any(GetUserByAccessKeyRequestDTO.class)))
                .thenAnswer((Answer<GetUserByAccessKeyResponseDTO>) innovation -> {
                    final GetUserByAccessKeyResponseDTO response = new GetUserByAccessKeyResponseDTO();
                    final UserData user = new UserData();
                    user.setName(TEST_USER_ID);
                    user.setParentId(SAMPLE_TENANT_ID);
                    response.setData(user);
                    return response;
                });

        // Run the test
        scalityOsisServiceUnderTest.deleteS3Credential(null, null, TEST_ACCESS_KEY);

        // Verify the results
        verify(iamMock).deleteAccessKey(any(DeleteAccessKeyRequest.class));
        verify(redisRepositoryMock).delete(any());
    }

    @Test
    void testDeleteS3CredentialEmptyAccessKey() {
        // Setup

        // Run the test
        scalityOsisServiceUnderTest.deleteS3Credential(TEST_TENANT_ID, TEST_USER_ID, "");

        // Verify the results
        verify(iamMock, never()).deleteAccessKey(any(DeleteAccessKeyRequest.class));
        verify(redisRepositoryMock, never()).delete(any());
    }

    @Test
    void testDeleteS3CredentialNoAdminPolicy() {
        // Setup
        when(iamMock.deleteAccessKey(any(DeleteAccessKeyRequest.class)))
                .thenAnswer((Answer<DeleteAccessKeyResult>) invocation -> {
                    final AmazonIdentityManagementException iamException = new AmazonIdentityManagementException(
                            "Forbidden");
                    iamException.setStatusCode(HttpStatus.FORBIDDEN.value());
                    throw iamException;
                })
                .thenAnswer((Answer<DeleteAccessKeyResult>) invocation -> new DeleteAccessKeyResult());


        // Run the test
        scalityOsisServiceUnderTest.deleteS3Credential(TEST_TENANT_ID, TEST_USER_ID, TEST_ACCESS_KEY);

        // Verify the results
        verify(redisRepositoryMock).delete(any());
    }

    @Test
    void testGetS3CredentialActive() {
        // Setup

        // Run the test
        final OsisS3Credential result = scalityOsisServiceUnderTest.getS3Credential(SAMPLE_TENANT_ID, TEST_USER_ID,
                TEST_ACCESS_KEY);

        // Verify the results
        assertEquals(TEST_USER_ID, result.getCdUserId());
        assertEquals(TEST_USER_ID, result.getUserId());
        assertEquals(SAMPLE_TENANT_ID, result.getTenantId());
        assertEquals(TEST_ACCESS_KEY, result.getAccessKey());
        assertEquals(TEST_SECRET_KEY, result.getSecretKey());
        assertTrue(result.getActive());
        assertNotNull(result.getCreationDate());
    }

    @Test
    void testGetS3CredentialInactive() {
        // Setup
        when(iamMock.listAccessKeys(any(ListAccessKeysRequest.class)))
                .thenAnswer((Answer<ListAccessKeysResult>) this::listInactiveAccessKeysMockResponse);

        // Run the test
        final OsisS3Credential result = scalityOsisServiceUnderTest.getS3Credential(SAMPLE_TENANT_ID, TEST_USER_ID,
                TEST_ACCESS_KEY);

        // Verify the results
        assertEquals(TEST_USER_ID, result.getCdUserId());
        assertEquals(TEST_USER_ID, result.getUserId());
        assertEquals(SAMPLE_TENANT_ID, result.getTenantId());
        assertEquals(TEST_ACCESS_KEY, result.getAccessKey());
        assertEquals(TEST_SECRET_KEY, result.getSecretKey());
        assertFalse(result.getActive());
        assertNotNull(result.getCreationDate());
    }

    @Test
    void testGetS3CredentialWithOnlyAccessKeyActive() {
        // Setup
        when(vaultAdminMock.getUserByAccessKey(any(GetUserByAccessKeyRequestDTO.class)))
                .thenAnswer((Answer<GetUserByAccessKeyResponseDTO>) innovation -> {
                    final GetUserByAccessKeyResponseDTO response = new GetUserByAccessKeyResponseDTO();
                    final UserData user = new UserData();
                    user.setName(TEST_USER_ID);
                    user.setParentId(SAMPLE_TENANT_ID);
                    response.setData(user);
                    return response;
                });

        // Run the test
        final OsisS3Credential result = scalityOsisServiceUnderTest.getS3Credential(TEST_ACCESS_KEY);

        // Verify the results
        assertEquals(TEST_USER_ID, result.getUserId());
        assertEquals(SAMPLE_TENANT_ID, result.getTenantId());
        assertEquals(TEST_ACCESS_KEY, result.getAccessKey());
        assertEquals(TEST_SECRET_KEY, result.getSecretKey());
        assertTrue(result.getActive());
    }

    @Test
    void testGetS3CredentialWithOnlyAccessKeyInactive() {
        // Setup
        when(vaultAdminMock.getUserByAccessKey(any(GetUserByAccessKeyRequestDTO.class)))
                .thenAnswer((Answer<GetUserByAccessKeyResponseDTO>) innovation -> {
                    final GetUserByAccessKeyResponseDTO response = new GetUserByAccessKeyResponseDTO();
                    final UserData user = new UserData();
                    user.setName(TEST_USER_ID);
                    user.setParentId(SAMPLE_TENANT_ID);
                    response.setData(user);
                    return response;
                });

        when(iamMock.listAccessKeys(any(ListAccessKeysRequest.class)))
                .thenAnswer((Answer<ListAccessKeysResult>) this::listInactiveAccessKeysMockResponse);

        // Run the test
        final OsisS3Credential result = scalityOsisServiceUnderTest.getS3Credential(TEST_ACCESS_KEY);

        // Verify the results
        assertEquals(TEST_USER_ID, result.getUserId());
        assertEquals(SAMPLE_TENANT_ID, result.getTenantId());
        assertEquals(TEST_ACCESS_KEY, result.getAccessKey());
        assertEquals(TEST_SECRET_KEY, result.getSecretKey());
        assertFalse(result.getActive());
    }

    @Test
    void testGetS3CredentialNoAdminPolicy() {
        // Setup
        when(iamMock.listAccessKeys(any(ListAccessKeysRequest.class)))
                .thenAnswer((Answer<ListAccessKeysResult>) invocation -> {
                    final AmazonIdentityManagementException iamException = new AmazonIdentityManagementException(
                            "Forbidden");
                    iamException.setStatusCode(HttpStatus.FORBIDDEN.value());
                    throw iamException;
                })
                .thenAnswer((Answer<ListAccessKeysResult>) this::listAccessKeysMockResponse);

        // Run the test
        final OsisS3Credential result = scalityOsisServiceUnderTest.getS3Credential(SAMPLE_TENANT_ID, TEST_USER_ID,
                TEST_ACCESS_KEY);

        // Verify the results
        assertEquals(TEST_USER_ID, result.getCdUserId());
        assertEquals(TEST_USER_ID, result.getUserId());
        assertEquals(SAMPLE_TENANT_ID, result.getTenantId());
        assertEquals(TEST_ACCESS_KEY, result.getAccessKey());
        assertEquals(TEST_SECRET_KEY, result.getSecretKey());
        assertTrue(result.getActive());
        assertNotNull(result.getCreationDate());
    }

    @Test
    void testGetS3CredentialWithOnlyAccessKeyNoAdminPolicy() {
        // Setup
        when(vaultAdminMock.getUserByAccessKey(any(GetUserByAccessKeyRequestDTO.class)))
                .thenAnswer((Answer<GetUserByAccessKeyResponseDTO>) innovation -> {
                    final GetUserByAccessKeyResponseDTO response = new GetUserByAccessKeyResponseDTO();
                    final UserData user = new UserData();
                    user.setName(TEST_USER_ID);
                    user.setParentId(SAMPLE_TENANT_ID);
                    response.setData(user);
                    return response;
                });

        when(iamMock.listAccessKeys(any(ListAccessKeysRequest.class)))
                .thenAnswer((Answer<ListAccessKeysResult>) invocation -> {
                    final AmazonIdentityManagementException iamException = new AmazonIdentityManagementException(
                            "Forbidden");
                    iamException.setStatusCode(HttpStatus.FORBIDDEN.value());
                    throw iamException;
                })
                .thenAnswer((Answer<ListAccessKeysResult>) this::listAccessKeysMockResponse);

        // Run the test
        final OsisS3Credential result = scalityOsisServiceUnderTest.getS3Credential(TEST_ACCESS_KEY);

        // Verify the results
        assertEquals(TEST_USER_ID, result.getUserId());
        assertEquals(SAMPLE_TENANT_ID, result.getTenantId());
        assertEquals(TEST_ACCESS_KEY, result.getAccessKey());
        assertEquals(TEST_SECRET_KEY, result.getSecretKey());
        assertTrue(result.getActive());
    }

    @Test
    void testGetS3CredentialWithNoKeyOnRedis() {
        // Setup
        when(redisRepositoryMock.hasKey(any())).thenReturn(Boolean.FALSE);

        // Run the test
        final OsisS3Credential result = scalityOsisServiceUnderTest.getS3Credential(SAMPLE_TENANT_ID, TEST_USER_ID,
                TEST_ACCESS_KEY);

        // Verify the results
        assertEquals(TEST_USER_ID, result.getCdUserId());
        assertEquals(TEST_USER_ID, result.getUserId());
        assertEquals(SAMPLE_TENANT_ID, result.getTenantId());
        assertEquals(TEST_ACCESS_KEY, result.getAccessKey());
        assertEquals(NOT_AVAILABLE, result.getSecretKey());
        assertTrue(result.getActive());
        assertNotNull(result.getCreationDate());
    }

    @Test
    void testGetS3CredentialNotFound() {
        // Setup

        // Run the test
        assertThrows(VaultServiceException.class,
                () -> scalityOsisServiceUnderTest.getS3Credential(SAMPLE_TENANT_ID, TEST_USER_ID, TEST_ACCESS_KEY_2));

        // Verify the results
    }

    @Test
    void testGetS3CredentialWithOnlyAccessKeyNotFound() {
        // Setup
        when(vaultAdminMock.getUserByAccessKey(any(GetUserByAccessKeyRequestDTO.class)))
                .thenAnswer((Answer<GetUserByAccessKeyResponseDTO>) innovation -> {
                    final GetUserByAccessKeyResponseDTO response = new GetUserByAccessKeyResponseDTO();
                    final UserData user = new UserData();
                    user.setName(TEST_USER_ID);
                    user.setParentId(SAMPLE_TENANT_ID);
                    response.setData(user);
                    return response;
                });

        // Run the test
        assertThrows(VaultServiceException.class,
                () -> scalityOsisServiceUnderTest.getS3Credential(TEST_ACCESS_KEY_2));

        // Verify the results
    }

    @Test
    void testGetS3CredentialWithNullTenantIdAndUserId() {
        // Setup
        when(vaultAdminMock.getUserByAccessKey(any(GetUserByAccessKeyRequestDTO.class)))
                .thenAnswer((Answer<GetUserByAccessKeyResponseDTO>) innovation -> {
                    final GetUserByAccessKeyResponseDTO response = new GetUserByAccessKeyResponseDTO();
                    final UserData user = new UserData();
                    user.setName(TEST_USER_ID);
                    user.setParentId(SAMPLE_TENANT_ID);
                    response.setData(user);
                    return response;
                });

        // Run the test
        final OsisS3Credential result = scalityOsisServiceUnderTest.getS3Credential(null, null, TEST_ACCESS_KEY);

        // Verify the results
        assertEquals(TEST_USER_ID, result.getUserId());
        assertEquals(SAMPLE_TENANT_ID, result.getTenantId());
        assertEquals(TEST_ACCESS_KEY, result.getAccessKey());
        assertEquals(TEST_SECRET_KEY, result.getSecretKey());
        assertTrue(result.getActive());
    }

    @Test
    void testListS3Credentials() {
        // Setup
        final long offset = 0L;
        final long limit = 1000L;

        // Run the test
        final PageOfS3Credentials pageOfS3Credentials = scalityOsisServiceUnderTest.listS3Credentials(TEST_TENANT_ID,
                TEST_USER_ID, offset, limit);

        // Verify the results
        assertNotNull(pageOfS3Credentials.getItems());
        assertEquals(TEST_TENANT_ID, pageOfS3Credentials.getItems().get(0).getTenantId());
        assertEquals(TEST_USER_ID, pageOfS3Credentials.getItems().get(0).getCdUserId());
        assertEquals(TEST_USER_ID, pageOfS3Credentials.getItems().get(0).getUserId());
        assertTrue(pageOfS3Credentials.getPageInfo().getTotal() > 0);
        assertEquals(offset, pageOfS3Credentials.getPageInfo().getOffset());
        assertEquals(limit, pageOfS3Credentials.getPageInfo().getLimit());
        assertFalse(pageOfS3Credentials.getItems().isEmpty());
    }

    @Test
    void testListS3CredentialsWithNoKeyOnRedis() {
        // Setup
        when(redisRepositoryMock.hasKey(any())).thenReturn(Boolean.FALSE);

        when(iamMock.listAccessKeys(any(ListAccessKeysRequest.class)))
                .thenAnswer((Answer<ListAccessKeysResult>) invocation -> {
                    final ListAccessKeysRequest request = invocation.getArgument(0);

                    final AccessKeyMetadata accessKeyMetadata = new AccessKeyMetadata()
                            .withAccessKeyId(TEST_ACCESS_KEY_2)
                            .withCreateDate(new Date())
                            .withStatus(StatusType.Active)
                            .withUserName(request.getUserName());

                    return new ListAccessKeysResult()
                            .withAccessKeyMetadata(Collections.singletonList(accessKeyMetadata));
                });

        final long offset = 0L;
        final long limit = 1000L;

        // Run the test
        final PageOfS3Credentials pageOfS3Credentials = scalityOsisServiceUnderTest.listS3Credentials(TEST_TENANT_ID,
                TEST_USER_ID, offset, limit);

        // Verify the results
        assertNotNull(pageOfS3Credentials.getItems());
        assertFalse(pageOfS3Credentials.getItems().isEmpty());

        // create-access-key should have been called
        verify(iamMock).createAccessKey(any());

        // First entry always should have secret key
        final OsisS3Credential resultWithSecret = pageOfS3Credentials.getItems().get(0);

        assertEquals(TEST_USER_ID, resultWithSecret.getCdUserId());
        assertEquals(TEST_USER_ID, resultWithSecret.getUserId());
        assertEquals(TEST_TENANT_ID, resultWithSecret.getTenantId());
        assertEquals(TEST_ACCESS_KEY, resultWithSecret.getAccessKey());
        assertEquals(TEST_SECRET_KEY, resultWithSecret.getSecretKey());

        // Last entry should have secret key as "Not Available"
        final OsisS3Credential resultWithNoSecret = pageOfS3Credentials.getItems()
                .get(pageOfS3Credentials.getItems().size() - 1);

        assertEquals(TEST_USER_ID, resultWithNoSecret.getCdUserId());
        assertEquals(TEST_USER_ID, resultWithNoSecret.getUserId());
        assertEquals(TEST_TENANT_ID, resultWithNoSecret.getTenantId());
        assertEquals(TEST_ACCESS_KEY_2, resultWithNoSecret.getAccessKey());
        assertEquals(NOT_AVAILABLE, resultWithNoSecret.getSecretKey());

    }

    @Test
    void testListS3CredentialsErr() {
        // Setup
        final long offset = 0L;
        final long limit = 1000L;
        when(iamMock.listAccessKeys(any(ListAccessKeysRequest.class)))
                .thenAnswer((Answer<ListAccessKeysResult>) invocation -> {
                    throw new NoSuchEntityException(
                            "The request was rejected because it referenced an entity that does not exist. The error message describes the entity.");
                });

        // Run the test
        final PageOfS3Credentials response = scalityOsisServiceUnderTest.listS3Credentials(TEST_TENANT_ID, TEST_USER_ID,
                offset, limit);

        // Verify the results
        assertEquals(0L, response.getPageInfo().getTotal());
        assertEquals(offset, response.getPageInfo().getOffset());
        assertEquals(limit, response.getPageInfo().getLimit());
        assertEquals(0L, response.getItems().size());

    }

    @Test
    void testListS3CredentialsNoAdminPolicy() throws Exception {
        // Setup
        final long offset = 0L;
        final long limit = 1000L;
        when(iamMock.listAccessKeys(any(ListAccessKeysRequest.class)))
                .thenAnswer((Answer<ListAccessKeysResult>) invocation -> {
                    final AmazonIdentityManagementException iamException = new AmazonIdentityManagementException(
                            "Forbidden");
                    iamException.setStatusCode(HttpStatus.FORBIDDEN.value());
                    throw iamException;
                })
                .thenAnswer((Answer<ListAccessKeysResult>) invocation -> listAccessKeysMockResponse(invocation));

        // Run the test
        final PageOfS3Credentials pageOfS3Credentials = scalityOsisServiceUnderTest.listS3Credentials(TEST_TENANT_ID,
                TEST_USER_ID, offset, limit);

        // Verify the results
        assertNotNull(pageOfS3Credentials.getItems());
        assertEquals(TEST_TENANT_ID, pageOfS3Credentials.getItems().get(0).getTenantId());
        assertEquals(TEST_USER_ID, pageOfS3Credentials.getItems().get(0).getCdUserId());
        assertEquals(TEST_USER_ID, pageOfS3Credentials.getItems().get(0).getUserId());
        assertTrue(pageOfS3Credentials.getPageInfo().getTotal() > 0);
        assertEquals(offset, pageOfS3Credentials.getPageInfo().getOffset());
        assertEquals(limit, pageOfS3Credentials.getPageInfo().getLimit());
        assertFalse(pageOfS3Credentials.getItems().isEmpty());

    }

    @Test
    void testUpdateCredentialDeactivate() {
        // Setup
        final OsisS3Credential osisS3Credential = new OsisS3Credential();
        osisS3Credential.setUserId(TEST_USER_ID);
        osisS3Credential.setTenantId(SAMPLE_TENANT_ID);
        osisS3Credential.setActive(false);
        osisS3Credential.setCdUserId(TEST_USER_ID);
        osisS3Credential.setAccessKey(TEST_ACCESS_KEY);

        when(iamMock.listAccessKeys(any(ListAccessKeysRequest.class)))
                .thenAnswer((Answer<ListAccessKeysResult>) this::listInactiveAccessKeysMockResponse);

        // Run the test
        final OsisS3Credential resS3Credential = scalityOsisServiceUnderTest.updateCredentialStatus(SAMPLE_TENANT_ID, TEST_USER_ID, TEST_ACCESS_KEY, osisS3Credential);

        // Verify the results
        assertFalse(resS3Credential.getActive());
    }

    @Test
    void testUpdateCredentialActivate() {
        // Setup
        final OsisS3Credential osisS3Credential = new OsisS3Credential();
        osisS3Credential.setUserId(TEST_USER_ID);
        osisS3Credential.setTenantId(SAMPLE_TENANT_ID);
        osisS3Credential.setActive(true);
        osisS3Credential.setCdUserId(TEST_USER_ID);
        osisS3Credential.setAccessKey(TEST_ACCESS_KEY);

        // Run the test
        final OsisS3Credential resS3Credential = scalityOsisServiceUnderTest.updateCredentialStatus(SAMPLE_TENANT_ID, TEST_USER_ID, TEST_ACCESS_KEY, osisS3Credential);

        // Verify the results
        assertTrue(resS3Credential.getActive());
    }

    @Test
    void testUpdateCredentialDeactivateNoAdminPolicy() throws Exception {
        // Setup
        final OsisS3Credential osisS3Credential = new OsisS3Credential();
        osisS3Credential.setUserId(TEST_USER_ID);
        osisS3Credential.setTenantId(SAMPLE_TENANT_ID);
        osisS3Credential.setActive(false);
        osisS3Credential.setCdUserId(TEST_USER_ID);
        osisS3Credential.setAccessKey(TEST_ACCESS_KEY);

        when(iamMock.updateAccessKey(any(UpdateAccessKeyRequest.class)))
                .thenAnswer((Answer<UpdateAccessKeyResult>) invocation -> {
                    final AmazonIdentityManagementException iamException = new AmazonIdentityManagementException(
                            "Forbidden");
                    iamException.setStatusCode(HttpStatus.FORBIDDEN.value());
                    throw iamException;
                })
                .thenAnswer((Answer<UpdateAccessKeyResult>) invocation -> new UpdateAccessKeyResult());

        when(iamMock.listAccessKeys(any(ListAccessKeysRequest.class)))
                .thenAnswer((Answer<ListAccessKeysResult>) this::listInactiveAccessKeysMockResponse);

        // Run the test
        final OsisS3Credential resS3CredentialActive = scalityOsisServiceUnderTest.updateCredentialStatus(SAMPLE_TENANT_ID,
                TEST_USER_ID, TEST_ACCESS_KEY, osisS3Credential);

        // Verify the results
        assertNotNull(resS3CredentialActive);
        assertFalse(resS3CredentialActive.getActive());
    }

    @Test
    void testUpdateCredentialActivateNoAdminPolicy() throws Exception {
        // Setup
        final OsisS3Credential osisS3Credential = new OsisS3Credential();
        osisS3Credential.setUserId(TEST_USER_ID);
        osisS3Credential.setTenantId(SAMPLE_TENANT_ID);
        osisS3Credential.setActive(true);
        osisS3Credential.setCdUserId(TEST_USER_ID);
        osisS3Credential.setAccessKey(TEST_ACCESS_KEY);

        when(iamMock.updateAccessKey(any(UpdateAccessKeyRequest.class)))
                .thenAnswer((Answer<UpdateAccessKeyResult>) invocation -> {
                    final AmazonIdentityManagementException iamException = new AmazonIdentityManagementException(
                            "Forbidden");
                    iamException.setStatusCode(HttpStatus.FORBIDDEN.value());
                    throw iamException;
                })
                .thenAnswer((Answer<UpdateAccessKeyResult>) invocation -> new UpdateAccessKeyResult());


        // Run the test
        final OsisS3Credential resS3CredentialInavtive = scalityOsisServiceUnderTest.updateCredentialStatus(SAMPLE_TENANT_ID,
                TEST_USER_ID, TEST_ACCESS_KEY, osisS3Credential);

        // Verify the results
        assertNotNull(resS3CredentialInavtive);
        assertTrue(resS3CredentialInavtive.getActive());
    }

    @Test
    void testUpdateCredentialDeactivateWithoutTenantIdAndUserId() {
        // Setup
        final OsisS3Credential osisS3Credential = new OsisS3Credential();
        osisS3Credential.setActive(false);

        when(vaultAdminMock.getUserByAccessKey(any(GetUserByAccessKeyRequestDTO.class)))
                .thenAnswer((Answer<GetUserByAccessKeyResponseDTO>) innovation -> {
                    final GetUserByAccessKeyResponseDTO response = new GetUserByAccessKeyResponseDTO();
                    final UserData user = new UserData();
                    user.setName(TEST_USER_ID);
                    user.setParentId(SAMPLE_TENANT_ID);
                    response.setData(user);
                    return response;
                });

        when(iamMock.listAccessKeys(any(ListAccessKeysRequest.class)))
                .thenAnswer((Answer<ListAccessKeysResult>) this::listInactiveAccessKeysMockResponse);

        // Run the test
        final OsisS3Credential resS3Credential = scalityOsisServiceUnderTest.updateCredentialStatus(null, null, TEST_ACCESS_KEY, osisS3Credential);

        // Verify the results
        assertEquals(resS3Credential.getTenantId(), SAMPLE_TENANT_ID);
        assertEquals(resS3Credential.getUserId(), TEST_USER_ID);
        assertFalse(resS3Credential.getActive());
    }

    @Test
    void testUpdateCredentialActivateWithoutTenantIdAndUserId() {
        // Setup
        final OsisS3Credential osisS3Credential = new OsisS3Credential();
        osisS3Credential.setActive(true);

        when(vaultAdminMock.getUserByAccessKey(any(GetUserByAccessKeyRequestDTO.class)))
                .thenAnswer((Answer<GetUserByAccessKeyResponseDTO>) innovation -> {
                    final GetUserByAccessKeyResponseDTO response = new GetUserByAccessKeyResponseDTO();
                    final UserData user = new UserData();
                    user.setName(TEST_USER_ID);
                    user.setParentId(SAMPLE_TENANT_ID);
                    response.setData(user);
                    return response;
                });

        // Run the test
        final OsisS3Credential resS3Credential = scalityOsisServiceUnderTest.updateCredentialStatus(null, null, TEST_ACCESS_KEY, osisS3Credential);

        // Verify the results
        assertEquals(resS3Credential.getTenantId(), SAMPLE_TENANT_ID);
        assertEquals(resS3Credential.getUserId(), TEST_USER_ID);
        assertTrue(resS3Credential.getActive());
    }

    @Test
    void testUpdateCredential400() {
        // Setup
        when(iamMock.updateAccessKey(any(UpdateAccessKeyRequest.class)))
                .thenAnswer((Answer<UpdateAccessKeyResult>) invocation -> {
                    throw new VaultServiceException(HttpStatus.BAD_REQUEST, "Bad Request");
                });

        // Run the test
        // Verify the results
        assertThrows(VaultServiceException.class, () -> {
            scalityOsisServiceUnderTest.updateCredentialStatus(TEST_TENANT_ID,
                    TEST_USER_ID, TEST_ACCESS_KEY, new OsisS3Credential().active(true));
        });
    }
}
