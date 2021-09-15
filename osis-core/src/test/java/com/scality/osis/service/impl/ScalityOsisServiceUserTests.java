package com.scality.osis.service.impl;

import com.amazonaws.services.identitymanagement.model.*;
import com.vmware.osis.model.*;
import com.vmware.osis.model.exception.*;
import com.scality.vaultclient.dto.*;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import com.scality.osis.vaultadmin.impl.*;
import org.springframework.http.HttpStatus;

import java.util.*;

import static com.scality.osis.utils.ScalityConstants.*;
import static com.scality.osis.utils.ScalityTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ScalityOsisServiceUserTests extends BaseOsisServiceTest{

    @Test
    public void testCreateUser() {
        // Setup
        final OsisUser osisUser = new OsisUser();
        osisUser.setUserId(TEST_USER_ID);
        osisUser.setCanonicalUserId(TEST_USER_ID);
        osisUser.setTenantId(TEST_TENANT_ID);
        osisUser.setCdTenantId(TEST_TENANT_ID);
        osisUser.setActive(true);
        osisUser.setCdUserId(TEST_USER_ID);
        osisUser.setRole(OsisUser.RoleEnum.TENANT_USER);
        osisUser.setUsername(TEST_NAME);

        // Run the test
        final OsisUser result = scalityOsisServiceUnderTest.createUser(osisUser);

        // Verify the results
        assertEquals(TEST_USER_ID, result.getCdUserId());
        assertEquals(TEST_USER_ID, result.getUserId());
        assertEquals(TEST_NAME, result.getUsername());
        assertEquals(TEST_TENANT_ID, result.getCdTenantId());
        assertEquals(TEST_TENANT_ID, result.getTenantId());
        assertEquals(TEST_NAME, result.getOsisS3Credentials().get(0).getUsername(), "Invalid getOsisS3Credentials username");
        assertEquals(TEST_USER_ID, result.getOsisS3Credentials().get(0).getUserId(), "Invalid getOsisS3Credentials getUserId");
        assertEquals(TEST_USER_ID, result.getOsisS3Credentials().get(0).getCdUserId(), "Invalid getOsisS3Credentials getCdUserId");
        assertEquals(TEST_SECRET_KEY, result.getOsisS3Credentials().get(0).getSecretKey(), "Invalid getOsisS3Credentials getSecretKey");
        assertEquals(TEST_ACCESS_KEY, result.getOsisS3Credentials().get(0).getAccessKey(), "Invalid getOsisS3Credentials getAccessKey");
        assertEquals(TEST_TENANT_ID, result.getOsisS3Credentials().get(0).getTenantId(), "Invalid getOsisS3Credentials getTenantId");
        assertEquals(TEST_TENANT_ID, result.getOsisS3Credentials().get(0).getCdTenantId(), "Invalid getOsisS3Credentials getCdTenantId");
        assertTrue(result.getActive());
    }

    @Test
    public void testCreateUser400() {
        // Setup
        when(iamMock.createUser(any(CreateUserRequest.class)))
                .thenAnswer((Answer<CreateUserResult>) invocation -> {
                    throw new VaultServiceException(HttpStatus.BAD_REQUEST, "Bad Request");
                });

        // Run the test
        // Verify the results
        assertThrows(VaultServiceException.class, () -> {
            scalityOsisServiceUnderTest.createUser(new OsisUser());
        });
    }

    @Test
    public void testCreateUserNoAdminPolicy() throws Exception {
        // Setup
        final OsisUser osisUser = new OsisUser();
        osisUser.setUserId(TEST_USER_ID);
        osisUser.setCanonicalUserId(TEST_USER_ID);
        osisUser.setTenantId(TEST_TENANT_ID);
        osisUser.setCdTenantId(TEST_TENANT_ID);
        osisUser.setActive(true);
        osisUser.setCdUserId(TEST_USER_ID);
        osisUser.setRole(OsisUser.RoleEnum.TENANT_USER);
        osisUser.setUsername(TEST_NAME);

        when(iamMock.createUser(any(CreateUserRequest.class)))
                .thenAnswer((Answer<CreateUserResult>) invocation -> {
                    final AmazonIdentityManagementException iamException = new AmazonIdentityManagementException("Forbidden");
                    iamException.setStatusCode(HttpStatus.FORBIDDEN.value());
                    throw iamException;
                })
                .thenAnswer((Answer<CreateUserResult>) invocation -> createUserMockResponse(invocation));

        // Run the test
        final OsisUser result = scalityOsisServiceUnderTest.createUser(osisUser);

        // Verify the results
        assertEquals(TEST_USER_ID, result.getCdUserId());
        assertEquals(TEST_USER_ID, result.getUserId());
        assertEquals(TEST_NAME, result.getUsername());
        assertEquals(TEST_TENANT_ID, result.getCdTenantId());
        assertEquals(TEST_TENANT_ID, result.getTenantId());
        assertEquals(TEST_NAME, result.getOsisS3Credentials().get(0).getUsername(), "Invalid getOsisS3Credentials username");
        assertEquals(TEST_USER_ID, result.getOsisS3Credentials().get(0).getUserId(), "Invalid getOsisS3Credentials getUserId");
        assertEquals(TEST_USER_ID, result.getOsisS3Credentials().get(0).getCdUserId(), "Invalid getOsisS3Credentials getCdUserId");
        assertEquals(TEST_SECRET_KEY, result.getOsisS3Credentials().get(0).getSecretKey(), "Invalid getOsisS3Credentials getSecretKey");
        assertEquals(TEST_ACCESS_KEY, result.getOsisS3Credentials().get(0).getAccessKey(), "Invalid getOsisS3Credentials getAccessKey");
        assertEquals(TEST_TENANT_ID, result.getOsisS3Credentials().get(0).getTenantId(), "Invalid getOsisS3Credentials getTenantId");
        assertEquals(TEST_TENANT_ID, result.getOsisS3Credentials().get(0).getCdTenantId(), "Invalid getOsisS3Credentials getCdTenantId");
        assertTrue(result.getActive());
    }

    @Test
    public void testQueryUsers() {
        // Setup
        final long offset = 0L;
        final long limit = 1000L;
        final String filter = CD_TENANT_ID_PREFIX + SAMPLE_CD_TENANT_ID + FILTER_SEPARATOR + DISPLAY_NAME_PREFIX + TEST_NAME;
        // cd_tenant_id==e7ecb16e-f6b7-4d34-ad4e-5da5d5c8317;display_name%3D%3D==name

        when(vaultAdminMock.getAccountID(any(ListAccountsRequestDTO.class))).thenReturn(SAMPLE_TENANT_ID);

        // Run the test
        final PageOfUsers response = scalityOsisServiceUnderTest.queryUsers(offset, limit, filter);

        // Verify the results
        assertEquals(1, response.getPageInfo().getTotal());
        assertEquals(offset, response.getPageInfo().getOffset());
        assertEquals(limit, response.getPageInfo().getLimit());
        assertEquals(1, response.getItems().size());
        assertEquals(TEST_NAME, response.getItems().get(0).getUsername());
        assertTrue(response.getItems().get(0).getTenantId().contains(SAMPLE_TENANT_ID));
    }

    @Test
    public void testQueryUsersFilterDifferentOrder() {
        // Setup
        final long offset = 0L;
        final long limit = 1000L;
        final String filter =  DISPLAY_NAME_PREFIX + TEST_NAME + FILTER_SEPARATOR +  CD_TENANT_ID_PREFIX + SAMPLE_CD_TENANT_ID;

        when(vaultAdminMock.getAccountID(any(ListAccountsRequestDTO.class))).thenReturn(SAMPLE_TENANT_ID);

        // Run the test
        final PageOfUsers response = scalityOsisServiceUnderTest.queryUsers(offset, limit, filter);

        // Verify the results
        assertEquals(1, response.getPageInfo().getTotal());
        assertEquals(offset, response.getPageInfo().getOffset());
        assertEquals(limit, response.getPageInfo().getLimit());
        assertEquals(1, response.getItems().size());
        assertEquals(TEST_NAME, response.getItems().get(0).getUsername());
        assertTrue(response.getItems().get(0).getTenantId().contains(SAMPLE_TENANT_ID));
    }

    @Test
    public void testQueryUsersFilterWithTenantID() {
        // Setup
        final long offset = 0L;
        final long limit = 1000L;
        final String filter = TENANT_ID_PREFIX + SAMPLE_TENANT_ID + FILTER_SEPARATOR + CD_TENANT_ID_PREFIX + SAMPLE_CD_TENANT_ID + FILTER_SEPARATOR + DISPLAY_NAME_PREFIX + TEST_NAME;

        // Run the test
        final PageOfUsers response = scalityOsisServiceUnderTest.queryUsers(offset, limit, filter);

        //verify getAccount never called as tenantID is present
        verify(vaultAdminMock, never()).getAccountID(any());

        // Verify the results
        assertEquals(1, response.getPageInfo().getTotal());
        assertEquals(offset, response.getPageInfo().getOffset());
        assertEquals(limit, response.getPageInfo().getLimit());
        assertEquals(1, response.getItems().size());
        assertEquals(TEST_NAME, response.getItems().get(0).getUsername());
        assertTrue(response.getItems().get(0).getTenantId().contains(SAMPLE_TENANT_ID));
    }

    @Test
    public void testQueryUsersFilterWithUsername() {
        // Setup
        final long offset = 0L;
        final long limit = 1000L;
        final String filter = CD_TENANT_ID_PREFIX + SAMPLE_CD_TENANT_ID + FILTER_SEPARATOR + USERNAME_PREFIX + TEST_NAME;

        when(vaultAdminMock.getAccountID(any(ListAccountsRequestDTO.class))).thenReturn(SAMPLE_TENANT_ID);

        // Run the test
        final PageOfUsers response = scalityOsisServiceUnderTest.queryUsers(offset, limit, filter);

        // Verify the results
        assertEquals(1, response.getPageInfo().getTotal());
        assertEquals(offset, response.getPageInfo().getOffset());
        assertEquals(limit, response.getPageInfo().getLimit());
        assertEquals(1, response.getItems().size());
        assertEquals(TEST_NAME, response.getItems().get(0).getUsername());
        assertTrue(response.getItems().get(0).getTenantId().contains(SAMPLE_TENANT_ID));
    }

    @Test
    public void testQueryUsersFilterWithUserid() {
        // Setup
        final long offset = 0L;
        final long limit = 1000L;
        final String filter = CD_TENANT_ID_PREFIX + SAMPLE_CD_TENANT_ID + FILTER_SEPARATOR + USER_ID_PREFIX + TEST_USER_ID;

        when(vaultAdminMock.getAccountID(any(ListAccountsRequestDTO.class))).thenReturn(SAMPLE_TENANT_ID);

        // Run the test
        final PageOfUsers response = scalityOsisServiceUnderTest.queryUsers(offset, limit, filter);

        // Verify the results
        assertEquals(1, response.getPageInfo().getTotal());
        assertEquals(offset, response.getPageInfo().getOffset());
        assertEquals(limit, response.getPageInfo().getLimit());
        assertEquals(1, response.getItems().size());
        assertEquals(TEST_USER_ID, response.getItems().get(0).getUserId());
        assertTrue(response.getItems().get(0).getTenantId().contains(SAMPLE_TENANT_ID));
    }

    @Test
    public void testQueryUsersFilterWithCdUserid() {
        // Setup
        final long offset = 0L;
        final long limit = 1000L;
        final String filter = CD_TENANT_ID_PREFIX + SAMPLE_CD_TENANT_ID + FILTER_SEPARATOR + CD_USER_ID_PREFIX + TEST_USER_ID;

        when(vaultAdminMock.getAccountID(any(ListAccountsRequestDTO.class))).thenReturn(SAMPLE_TENANT_ID);

        // Run the test
        final PageOfUsers response = scalityOsisServiceUnderTest.queryUsers(offset, limit, filter);

        // Verify the results
        assertEquals(1, response.getPageInfo().getTotal());
        assertEquals(offset, response.getPageInfo().getOffset());
        assertEquals(limit, response.getPageInfo().getLimit());
        assertEquals(1, response.getItems().size());
        assertEquals(TEST_USER_ID, response.getItems().get(0).getUserId());
        assertTrue(response.getItems().get(0).getTenantId().contains(SAMPLE_TENANT_ID));
    }

    @Test
    public void testQueryUsersWithNonUUID() {
        // Setup
        final long offset = 0L;
        final long limit = 1000L;
        final String filter = CD_TENANT_ID_PREFIX + SAMPLE_TENANT_ID + FILTER_SEPARATOR + DISPLAY_NAME_PREFIX + TEST_NAME;
        // cd_tenant_id==e7ecb16e-f6b7-4d34-ad4e-5da5d5c8317;display_name%3D%3D==name

        // Run the test
        final PageOfUsers response = scalityOsisServiceUnderTest.queryUsers(offset, limit, filter);

        // Verify the results
        assertEquals(1, response.getPageInfo().getTotal());
        assertEquals(offset, response.getPageInfo().getOffset());
        assertEquals(limit, response.getPageInfo().getLimit());
        assertEquals(1, response.getItems().size());
        assertEquals(TEST_NAME, response.getItems().get(0).getUsername());
        assertTrue(response.getItems().get(0).getTenantId().contains(SAMPLE_TENANT_ID));
    }

    @Test
    public void testQueryUsersInvalidCdTenantID() {
        // Setup
        final long offset = 0L;
        final long limit = 1000L;
        final String filter = CD_TENANT_ID_PREFIX + SAMPLE_CD_TENANT_ID + FILTER_SEPARATOR + DISPLAY_NAME_PREFIX + TEST_NAME;
        // cd_tenant_id==e7ecb16e-f6b7-4d34-ad4e-5da5d5c8317;display_name%3D%3D==name

        when(vaultAdminMock.getAccountID(any(ListAccountsRequestDTO.class))).thenThrow(
                new VaultServiceException(HttpStatus.BAD_REQUEST, "Provided cd_tenant_id does not exist"));

        // Run the test
        final PageOfUsers response = scalityOsisServiceUnderTest.queryUsers(offset, limit, filter);

        // Verify the results
        assertEquals(0L, response.getPageInfo().getTotal());
        assertEquals(offset, response.getPageInfo().getOffset());
        assertEquals(limit, response.getPageInfo().getLimit());
        assertEquals(0L, response.getItems().size());
    }

    @Test
    public void testQueryUsersInvalidDisplayName() {
        // Setup
        final long offset = 0L;
        final long limit = 1000L;
        final String filter = CD_TENANT_ID_PREFIX + SAMPLE_CD_TENANT_ID + FILTER_SEPARATOR + DISPLAY_NAME_PREFIX + TEST_NAME;
        // cd_tenant_id==e7ecb16e-f6b7-4d34-ad4e-5da5d5c8317;display_name%3D%3D==name

        when(vaultAdminMock.getAccountID(any(ListAccountsRequestDTO.class))).thenReturn(SAMPLE_TENANT_ID);

        final ListUsersResult userNotFoundResponse = new ListUsersResult().withUsers(new ArrayList<>());
        when(iamMock.listUsers(any(ListUsersRequest.class))).thenReturn(userNotFoundResponse);

        // Run the test
        final PageOfUsers response = scalityOsisServiceUnderTest.queryUsers(offset, limit, filter);

        // Verify the results
        assertEquals(0L, response.getPageInfo().getTotal());
        assertEquals(offset, response.getPageInfo().getOffset());
        assertEquals(limit, response.getPageInfo().getLimit());
        assertEquals(0L, response.getItems().size());
    }

    @Test
    public void testCreateS3Credential() {
        // Setup

        // Run the test
        final OsisS3Credential response = scalityOsisServiceUnderTest.createS3Credential(SAMPLE_TENANT_ID, TEST_USER_ID);

        // Verify the results
        assertNotNull(response.getAccessKey());
        assertNotNull(response.getSecretKey());
        assertEquals(TEST_USER_ID, response.getUserId());
        assertEquals(TEST_USER_ID, response.getCdUserId());
        assertEquals(SAMPLE_TENANT_ID, response.getTenantId());
        assertEquals(SAMPLE_CD_TENANT_ID, response.getCdTenantId());
    }

    @Test
    public void testCreateS3CredentialErr() {
        // Setup
        when(iamMock.createAccessKey(any(CreateAccessKeyRequest.class)))
                .thenThrow(
                        new NoSuchEntityException("The request was rejected because it referenced an entity that does not exist. " +
                                "The error message describes the entity."));

        // Run the test
        // Verify the results
        assertThrows(VaultServiceException.class, () -> scalityOsisServiceUnderTest.createS3Credential(SAMPLE_TENANT_ID, TEST_USER_ID));

    }

    @Test
    public void testCreateS3CredentialNoAdminPolicy() throws Exception {
        // Setup
        when(iamMock.createAccessKey(any(CreateAccessKeyRequest.class)))
                .thenAnswer((Answer<CreateAccessKeyResult>) invocation -> {
                    final AmazonIdentityManagementException iamException = new AmazonIdentityManagementException("Forbidden");
                    iamException.setStatusCode(HttpStatus.FORBIDDEN.value());
                    throw iamException;
                })
                .thenAnswer((Answer<CreateAccessKeyResult>) invocation -> createAccessKeyMockResponse(invocation));

        /// Run the test
        final OsisS3Credential response = scalityOsisServiceUnderTest.createS3Credential(SAMPLE_TENANT_ID, TEST_USER_ID);

        // Verify the results
        assertNotNull(response.getAccessKey());
        assertNotNull(response.getSecretKey());
        assertEquals(TEST_USER_ID, response.getUserId());
        assertEquals(TEST_USER_ID, response.getCdUserId());
        assertEquals(SAMPLE_TENANT_ID, response.getTenantId());
        assertEquals(SAMPLE_CD_TENANT_ID, response.getCdTenantId());

    }

    @Test
    public void testQueryS3Credentials() {
        // Setup
        final String filter = TENANT_ID_PREFIX + TEST_TENANT_ID + FILTER_SEPARATOR + USER_ID_PREFIX + TEST_USER_ID ;
        final long offset = 0L;
        final long limit = 1000L;


        // Run the test
        final PageOfS3Credentials pageOfS3Credentials = scalityOsisServiceUnderTest.queryS3Credentials(offset, limit, filter);

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
    public void testQueryS3CredentialsAccessKeyFilter() {
        // Setup
        final String filter = TENANT_ID_PREFIX + TEST_TENANT_ID
                + FILTER_SEPARATOR
                + USER_ID_PREFIX + TEST_USER_ID
                +FILTER_SEPARATOR
                + OSIS_ACCESS_KEY + FILTER_KEY_VALUE_SEPARATOR + TEST_ACCESS_KEY;
        final long offset = 0L;
        final long limit = 1000L;


        // Run the test
        final PageOfS3Credentials pageOfS3Credentials = scalityOsisServiceUnderTest.queryS3Credentials(offset, limit, filter);

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
    public void testQueryS3CredentialsInvalidFilter() {
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
    public void testDeleteS3Credential() {
        // Setup

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.deleteS3Credential(TEST_TENANT_ID, TEST_USER_ID, "accessKey"), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

    @Test
    public void testDeleteUser() {
        // Setup

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.deleteUser(TEST_TENANT_ID, TEST_USER_ID, false), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

    @Test
    public void testGetS3Credential() {
        // Setup

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.getS3Credential("accessKey"), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

    @Test
    public void testGetS3Credential2() {
        // Setup

        // Run the test
        final OsisS3Credential result = scalityOsisServiceUnderTest.getS3Credential(SAMPLE_TENANT_ID, TEST_USER_ID, TEST_ACCESS_KEY);

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
    public void testGetS3Credential2NoAdminPolicy() throws Exception {
        // Setup
        when(iamMock.listAccessKeys(any(ListAccessKeysRequest.class)))
                .thenAnswer((Answer<ListAccessKeysResult>) invocation -> {
                    final AmazonIdentityManagementException iamException = new AmazonIdentityManagementException("Forbidden");
                    iamException.setStatusCode(HttpStatus.FORBIDDEN.value());
                    throw iamException;
                })
                .thenAnswer((Answer<ListAccessKeysResult>) this::listAccessKeysMockResponse);

        // Run the test
        final OsisS3Credential result = scalityOsisServiceUnderTest.getS3Credential(SAMPLE_TENANT_ID, TEST_USER_ID, TEST_ACCESS_KEY);

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
    public void testGetS3Credential2WithNoKeyOnRedis() {
        // Setup
        when(redisRepositoryMock.hasKey(any())).thenReturn(Boolean.FALSE);

        // Run the test
        final OsisS3Credential result = scalityOsisServiceUnderTest.getS3Credential(SAMPLE_TENANT_ID, TEST_USER_ID, TEST_ACCESS_KEY);

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
    public void testGetS3Credential2NotFound() {
        // Setup

        // Run the test
        assertThrows(VaultServiceException.class, () ->  scalityOsisServiceUnderTest.getS3Credential(SAMPLE_TENANT_ID, TEST_USER_ID, TEST_ACCESS_KEY_2));

        // Verify the results
    }

    @Test
    public void testGetS3Credential2EmptyUserID() {
        // Setup

        // Run the test
        assertThrows(VaultServiceException.class, () ->  scalityOsisServiceUnderTest.getS3Credential("", TEST_USER_ID, TEST_ACCESS_KEY_2));

        // Verify the results
    }

    @Test
    public void testGetS3Credential2EmptyTenantID() {
        // Setup

        // Run the test
        assertThrows(VaultServiceException.class, () ->  scalityOsisServiceUnderTest.getS3Credential(SAMPLE_TENANT_ID, "", TEST_ACCESS_KEY_2));

        // Verify the results
    }

    @Test
    public void testGetUserWithCanonicalID() {
        // Setup

        // Run the test
        final OsisUser osisUser = scalityOsisServiceUnderTest.getUser(TEST_CANONICAL_ID);

        // Verify the results
        assertEquals(SAMPLE_TENANT_ID, osisUser.getTenantId());
        assertEquals(SAMPLE_CD_TENANT_ID, osisUser.getCdTenantId());
        assertEquals(TEST_CANONICAL_ID, osisUser.getCanonicalUserId());
        assertNotNull(osisUser.getUserId());
        assertNotNull(osisUser.getCdUserId());
        assertNotNull(osisUser.getUsername());
        assertNotNull(osisUser.getRole());
        assertTrue(osisUser.getActive());

        // Verify the results
    }

    @Test
    public void testGetUserWithCanonicalIDErr() {
        // Setup
        when(vaultAdminMock.getAccount(any()))
                .thenAnswer((Answer<AccountData>) invocation -> {
                    throw new VaultServiceException(HttpStatus.NOT_FOUND, "The Entity doesn't exist");
                });

        // Run the test
        // Verify the results
        assertThrows(VaultServiceException.class, () -> scalityOsisServiceUnderTest.getUser(TEST_CANONICAL_ID));
    }

    @Test
    public void testGetUserWithUserID() {
        // Setup

        // Run the test
        final OsisUser osisUser = scalityOsisServiceUnderTest.getUser(TEST_TENANT_ID, TEST_USER_ID);

        // Verify the results
        assertEquals(TEST_TENANT_ID, osisUser.getTenantId());
        assertEquals(TEST_USER_ID, osisUser.getUserId());
        assertEquals(TEST_USER_ID, osisUser.getCdUserId());
        assertNotNull(osisUser.getUsername());
        assertNotNull(osisUser.getCdTenantId());
        assertNotNull(osisUser.getCanonicalUserId());
        assertNotNull(osisUser.getRole());
        assertNotNull(osisUser.getEmail());
        assertTrue(osisUser.getActive());
    }

    @Test
    public void testGetUserWithUserIDErr() {
        // Setup
        when(iamMock.getUser(any(GetUserRequest.class)))
                .thenAnswer((Answer<GetUserResult>) invocation -> {
                    throw new VaultServiceException(HttpStatus.BAD_REQUEST, "Bad Request");
                });

        // Run the test
        // Verify the results
        assertThrows(VaultServiceException.class, () -> scalityOsisServiceUnderTest.getUser(TEST_TENANT_ID, TEST_USER_ID));
    }

    @Test
    public void testGetUserWithUserIDNoUser() {
        // Setup
        when(iamMock.getUser(any(GetUserRequest.class)))
                .thenAnswer((Answer<GetUserResult>) invocation -> {
                    final GetUserRequest request = invocation.getArgument(0);
                    throw new NoSuchEntityException("The user with name " + request.getUserName() +" cannot be found.");
                });

        // Run the test
        // Verify the results
        assertThrows(VaultServiceException.class, () -> scalityOsisServiceUnderTest.getUser(TEST_TENANT_ID, TEST_USER_ID));
    }

    @Test
    public void testGetUserWithUserIDNoAdminPolicy() throws Exception {
        // Setup
        when(iamMock.getUser(any(GetUserRequest.class)))
                .thenAnswer((Answer<GetUserResult>) invocation -> {
                    final AmazonIdentityManagementException iamException = new AmazonIdentityManagementException("Forbidden");
                    iamException.setStatusCode(HttpStatus.FORBIDDEN.value());
                    throw iamException;
                })
                .thenAnswer((Answer<GetUserResult>) invocation -> getUserMockResponse(invocation));

        // Run the test
        final OsisUser osisUser = scalityOsisServiceUnderTest.getUser(TEST_TENANT_ID, TEST_USER_ID);

        // Verify the results
        assertEquals(TEST_TENANT_ID, osisUser.getTenantId());
        assertEquals(TEST_USER_ID, osisUser.getUserId());
        assertEquals(TEST_USER_ID, osisUser.getCdUserId());
        assertNotNull(osisUser.getUsername());
        assertNotNull(osisUser.getCdTenantId());
        assertNotNull(osisUser.getCanonicalUserId());
        assertNotNull(osisUser.getRole());
        assertNotNull(osisUser.getEmail());
        assertTrue(osisUser.getActive());
    }

    @Test
    public void testHeadUser() {
        // Setup

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.headUser(TEST_TENANT_ID, TEST_USER_ID), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results

    }

    @Test
    public void testListS3Credentials() {
        // Setup
        final long offset = 0L;
        final long limit = 1000L;


        // Run the test
        final PageOfS3Credentials pageOfS3Credentials = scalityOsisServiceUnderTest.listS3Credentials(TEST_TENANT_ID, TEST_USER_ID, offset, limit);

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
    public void testListS3CredentialsWithNoKeyOnRedis() {
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
        final PageOfS3Credentials pageOfS3Credentials = scalityOsisServiceUnderTest.listS3Credentials(TEST_TENANT_ID, TEST_USER_ID, offset, limit);

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
        final OsisS3Credential resultWithNoSecret = pageOfS3Credentials.getItems().get(pageOfS3Credentials.getItems().size()-1);

        assertEquals(TEST_USER_ID, resultWithNoSecret.getCdUserId());
        assertEquals(TEST_USER_ID , resultWithNoSecret.getUserId());
        assertEquals(TEST_TENANT_ID, resultWithNoSecret.getTenantId());
        assertEquals(TEST_ACCESS_KEY_2, resultWithNoSecret.getAccessKey());
        assertEquals(NOT_AVAILABLE, resultWithNoSecret.getSecretKey());

    }

    @Test
    public void testListS3CredentialsErr() {
        // Setup
        final long offset = 0L;
        final long limit = 1000L;
        when(iamMock.listAccessKeys(any(ListAccessKeysRequest.class)))
                .thenAnswer((Answer<ListAccessKeysResult>) invocation -> {
                    throw new NoSuchEntityException("The request was rejected because it referenced an entity that does not exist. The error message describes the entity.");
                });

        // Run the test
        final PageOfS3Credentials response = scalityOsisServiceUnderTest.listS3Credentials(TEST_TENANT_ID, TEST_USER_ID, offset, limit);

        // Verify the results
        assertEquals(0L, response.getPageInfo().getTotal());
        assertEquals(offset, response.getPageInfo().getOffset());
        assertEquals(limit, response.getPageInfo().getLimit());
        assertEquals(0L, response.getItems().size());

    }

    @Test
    public void testListS3CredentialsNoAdminPolicy() throws Exception {
        // Setup
        final long offset = 0L;
        final long limit = 1000L;
        when(iamMock.listAccessKeys(any(ListAccessKeysRequest.class)))
                .thenAnswer((Answer<ListAccessKeysResult>) invocation -> {
                    final AmazonIdentityManagementException iamException = new AmazonIdentityManagementException("Forbidden");
                    iamException.setStatusCode(HttpStatus.FORBIDDEN.value());
                    throw iamException;
                })
                .thenAnswer((Answer<ListAccessKeysResult>) invocation -> listAccessKeysMockResponse(invocation));

        // Run the test
        final PageOfS3Credentials pageOfS3Credentials = scalityOsisServiceUnderTest.listS3Credentials(TEST_TENANT_ID, TEST_USER_ID, offset, limit);

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
    public void testListUsers() {
        // Setup
        final long offset = 0L;
        final long limit = 1000L;

        // Run the test
        final PageOfUsers response = scalityOsisServiceUnderTest.listUsers(SAMPLE_TENANT_ID, offset, limit);

        // Verify the results
        assertEquals(limit, response.getPageInfo().getTotal());
        assertEquals(offset, response.getPageInfo().getOffset());
        assertEquals(limit, response.getPageInfo().getLimit());
        assertEquals((int)limit, response.getItems().size());
    }

    @Test
    public void testListUsersWithOffset() {
        // Setup
        final long offset = 2000L;
        final long limit = 1000L;

        // Run the test
        final PageOfUsers response = scalityOsisServiceUnderTest.listUsers(SAMPLE_TENANT_ID, offset, limit);

        // Verify the results
        assertEquals(limit, response.getPageInfo().getTotal());
        assertEquals(offset, response.getPageInfo().getOffset());
        assertEquals(limit, response.getPageInfo().getLimit());
        assertEquals((int)limit, response.getItems().size());
    }

    @Test
    public void testListUsersErr() {
        // Setup
        final long offset = 0L;
        final long limit = 1000L;
        when(iamMock.listUsers(any(ListUsersRequest.class)))
                .thenAnswer((Answer<ListUsersResult>) invocation -> {
                    throw new VaultServiceException(HttpStatus.BAD_REQUEST, "Requested offset is outside the total available items");
                });

        // Run the test
        final PageOfUsers response = scalityOsisServiceUnderTest.listUsers(SAMPLE_TENANT_ID, offset, limit);

        // Verify the results
        assertEquals(0L, response.getPageInfo().getTotal());
        assertEquals(offset, response.getPageInfo().getOffset());
        assertEquals(limit, response.getPageInfo().getLimit());
        assertEquals(0L, response.getItems().size());

    }

    @Test
    public void testListUsersNoAdminPolicy() throws Exception {
        // Setup
        final long offset = 0L;
        final long limit = 1000L;

        when(iamMock.listUsers(any(ListUsersRequest.class)))
                .thenAnswer((Answer<ListUsersResult>) invocation -> {
                    final AmazonIdentityManagementException iamException = new AmazonIdentityManagementException("Forbidden");
                    iamException.setStatusCode(HttpStatus.FORBIDDEN.value());
                    throw iamException;
                })
                .thenAnswer((Answer<ListUsersResult>) invocation -> listUsersMockResponse(invocation));

        // Run the test
        final PageOfUsers response = scalityOsisServiceUnderTest.listUsers(SAMPLE_TENANT_ID, offset, limit);

        // Verify the results
        assertEquals(limit, response.getPageInfo().getTotal());
        assertEquals(offset, response.getPageInfo().getOffset());
        assertEquals(limit, response.getPageInfo().getLimit());
        assertEquals((int)limit, response.getItems().size());

    }

    @Test
    public void testUpdateUser() {
        // Setup
        final OsisUser osisUser = new OsisUser();
        osisUser.userId(TEST_USER_ID);
        osisUser.setUserId(TEST_USER_ID);
        osisUser.canonicalUserId("canonicalUserId");
        osisUser.setCanonicalUserId("canonicalUserId");
        osisUser.tenantId(TEST_TENANT_ID);
        osisUser.setTenantId(TEST_TENANT_ID);
        osisUser.active(false);
        osisUser.setActive(false);
        osisUser.cdUserId("cdUserId");
        osisUser.setCdUserId("cdUserId");

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.updateUser(TEST_TENANT_ID, TEST_USER_ID, osisUser), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }


}
