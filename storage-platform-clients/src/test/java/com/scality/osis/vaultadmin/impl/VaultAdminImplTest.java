package com.scality.osis.vaultadmin.impl;

import com.amazonaws.Response;
import com.amazonaws.http.HttpResponse;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.scality.osis.vaultadmin.impl.cache.CacheFactory;
import com.scality.osis.vaultadmin.impl.cache.CacheImpl;
import com.scality.vaultclient.dto.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.*;

import static com.scality.osis.vaultadmin.impl.VaultAdminImpl.CD_TENANT_ID_PREFIX;
import static com.scality.osis.vaultadmin.impl.cache.CacheConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SuppressWarnings("ConstantConditions")
class VaultAdminImplTest extends BaseTest {
    public static final String CACHE_FACTORY = "cacheFactory";

    @Test
    void createAccount() {

        //vault specific email address format for ose-scality
        final String emailAddress = UUID.randomUUID().toString() +"_"+ UUID.randomUUID().toString() + "@osis.account.com";
        //vault specific name format for ose-scality
        final String name = "tenant.name__bfc0d4a51e06481cbc917e9d96e52d81";
        final CreateAccountRequestDTO createAccountRequestDTO = CreateAccountRequestDTO.builder()
                .emailAddress(emailAddress)
                .name(name)
                .build();

        final CreateAccountResponseDTO response = vaultAdminImpl.createAccount(createAccountRequestDTO);
        assertEquals(emailAddress, response.getAccount().getData().getEmailAddress());
        assertEquals(name, response.getAccount().getData().getName());
        assertNotNull(response.getAccount().getData().getArn());
        assertNotNull(response.getAccount().getData().getCreateDate());
        assertNotNull(response.getAccount().getData().getId());
        assertNotNull(response.getAccount().getData().getCanonicalId());
    }

    @Test
    void createAccountErrorExistingEntity() {

        //vault specific email address format for ose-scality
        final String emailAddress = UUID.randomUUID().toString() +"_"+ UUID.randomUUID().toString() + "@osis.account.com";
        //vault specific name format for ose-scality
        final String name = "tenant.name__bfc0d4a51e06481cbc917e9d96e52d81";

        final CreateAccountRequestDTO createAccountRequestDTO = CreateAccountRequestDTO.builder()
                .emailAddress(emailAddress)
                .name(name)
                .build();

        loadExistingAccountErrorMocks();

        final VaultServiceException exception = assertThrows(VaultServiceException.class, () -> {
            vaultAdminImpl.createAccount(createAccountRequestDTO);
        });
        assertEquals(409, exception.getStatus().value());
        assertEquals("EntityAlreadyExists", exception.getErrorCode());

        //reinit the default mocks
        initMocks();
    }

    @Test
    void createAccountIOException() {

        //vault specific email address format for ose-scality
        final String emailAddress = UUID.randomUUID().toString() +"_"+ UUID.randomUUID().toString() + "@osis.account.com";
        //vault specific name format for ose-scality
        final String name = "tenant.name__bfc0d4a51e06481cbc917e9d96e52d81";
        final CreateAccountRequestDTO createAccountRequestDTO = CreateAccountRequestDTO.builder()
                .emailAddress(emailAddress)
                .name(name)
                .build();

        when(accountServicesClient.createAccount(any(CreateAccountRequestDTO.class)))
                .thenAnswer((Answer<Response<CreateAccountResponseDTO>>) invocation -> {
                    final IOException ioException = new IOException("Invalid URL");
                    throw ioException;
                });

        final VaultServiceException exception = assertThrows(VaultServiceException.class, () -> {
            vaultAdminImpl.createAccount(createAccountRequestDTO);
        });
        assertEquals(500, exception.getStatus().value());
        assertEquals("Exception", exception.getReason());

        //reinit the default mocks
        initMocks();
    }

    @Test
    void createAccountErrServiceResponse() {

        //vault specific email address format for ose-scality
        final String emailAddress = UUID.randomUUID().toString() +"_"+ UUID.randomUUID().toString() + "@osis.account.com";
        //vault specific name format for ose-scality
        final String name = "tenant.name__bfc0d4a51e06481cbc917e9d96e52d81";
        final CreateAccountRequestDTO createAccountRequestDTO = CreateAccountRequestDTO.builder()
                .emailAddress(emailAddress)
                .name(name)
                .build();

        when(accountServicesClient.createAccount(any(CreateAccountRequestDTO.class)))
                .thenAnswer((Answer<Response<CreateAccountResponseDTO>>) invocation -> {
                    final HttpResponse   httpResponse = new HttpResponse(null, null);
                    httpResponse.setStatusCode(403);
                    httpResponse.setStatusText("Forbidden");
                    return new Response<>(null,httpResponse);
                });

        final VaultServiceException exception = assertThrows(VaultServiceException.class, () -> {
            vaultAdminImpl.createAccount(createAccountRequestDTO);
        });
        assertEquals(403, exception.getStatus().value());
        assertEquals("Forbidden", exception.getReason());

        //reinit the default mocks
        initMocks();
    }

    @Test
    void listAccounts() {

        final ListAccountsRequestDTO   listAccountsRequestDTO = ListAccountsRequestDTO.builder()
                .maxItems(5)
                .build();

        final ListAccountsResponseDTO response = vaultAdminImpl.listAccounts(listAccountsRequestDTO);
        assertNotEquals(0, response.getAccounts().size());
        assertEquals(response.getAccounts().get(0).getCustomAttributes(), null);
    }

    @Test
    void listAccounts400ServiceResponse() {

        final ListAccountsRequestDTO   listAccountsRequestDTO = ListAccountsRequestDTO.builder()
                .maxItems(5)
                .build();

        when(accountServicesClient.listAccounts(any(ListAccountsRequestDTO.class)))
                .thenAnswer((Answer<Response<ListAccountsResponseDTO>>) invocation -> {
                    final HttpResponse   httpResponse = new HttpResponse(null, null);
                    httpResponse.setStatusCode(400);
                    httpResponse.setStatusText("WrongFormat");
                    return new Response<>(null,httpResponse);
                });

        final VaultServiceException exception = assertThrows(VaultServiceException.class, () -> {
            vaultAdminImpl.listAccounts(listAccountsRequestDTO);
        });
        assertEquals(400, exception.getStatus().value());
        assertEquals("WrongFormat", exception.getReason());

        //reinit the default mocks
        initMocks();
    }

    @Test
    void listAccounts500ServiceResponse() {

        final ListAccountsRequestDTO   listAccountsRequestDTO = ListAccountsRequestDTO.builder()
                .maxItems(5)
                .build();

        when(accountServicesClient.listAccounts(any(ListAccountsRequestDTO.class)))
                .thenAnswer((Answer<Response<ListAccountsResponseDTO>>) invocation -> {
                    final HttpResponse   httpResponse = new HttpResponse(null, null);
                    httpResponse.setStatusCode(500);
                    httpResponse.setStatusText("ServiceFailure");
                    return new Response<>(null,httpResponse);
                });

        final VaultServiceException exception = assertThrows(VaultServiceException.class, () -> {
            vaultAdminImpl.listAccounts(listAccountsRequestDTO);
        });
        assertEquals(500, exception.getStatus().value());
        assertEquals("ServiceFailure", exception.getReason());

        //reinit the default mocks
        initMocks();
    }

    @Test
    void testGetAccountAccessKey() {

        final GenerateAccountAccessKeyRequest generateAccountAccessKeyRequest = GenerateAccountAccessKeyRequest.builder()
                .accountName(DEFAULT_TEST_ACCOUNT_ID)
                .durationSeconds(60L)
                .build();

        final GenerateAccountAccessKeyResponse response = vaultAdminImpl.getAccountAccessKey(generateAccountAccessKeyRequest);
        assertNotNull(response.getData());
        final AccountSecretKeyData secretKeyData = response.getData();
        assertNotNull(secretKeyData.getId());
        assertNotNull(secretKeyData.getValue());
        assertNotNull(secretKeyData.getNotAfter());
        assertEquals(ACTIVE_STR, secretKeyData.getStatus());
        assertNotNull(secretKeyData.getCreateDate());
        assertNotNull(secretKeyData.getLastUsedDate());
    }

    @Test
    void testVaultAdminImplInvalidEndpoint() {
        assertThrows(IllegalArgumentException.class,
                () -> new VaultAdminImpl(accountServicesClient, stsClient, "dummy_vault_admin_endpoint", s3InterfaceEndpoint),
                "VaultAdminImpl constructor should throw IllegalArgumentException for null endpoint");
    }

    @Test
    void testGetListAccountsMarkerCacheEmpty() {

        final CacheFactory cacheFactoryMock = Mockito.mock(CacheFactory.class);
        when(cacheFactoryMock.getCache(NAME_LIST_ACCOUNTS_CACHE)).thenReturn(new CacheImpl<>(DEFAULT_CACHE_MAX_CAPACITY));

        ReflectionTestUtils.setField(vaultAdminImpl, CACHE_FACTORY, cacheFactoryMock);
        vaultAdminImpl.initCaches();

        final String marker = vaultAdminImpl.getAccountsMarker(1000, CD_TENANT_ID_PREFIX);
        assertEquals("M1000", marker);
    }

    @Test
    void testGetListAccountsMarkerWithExistingCache1() {

        final CacheFactory cacheFactoryMock = Mockito.mock(CacheFactory.class);
        final CacheImpl<Integer, String> cache = new CacheImpl<>(DEFAULT_CACHE_MAX_CAPACITY);
        cache.put(1000,"M1000");
        cache.put(2000,"M2000");
        cache.put(4000,"M4000");
        when(cacheFactoryMock.getCache(NAME_LIST_ACCOUNTS_CACHE)).thenReturn(cache);

        ReflectionTestUtils.setField(vaultAdminImpl, CACHE_FACTORY, cacheFactoryMock);
        vaultAdminImpl.initCaches();

        final String marker = vaultAdminImpl.getAccountsMarker(3000, CD_TENANT_ID_PREFIX);
        assertEquals("M3000", marker);
    }

    @Test
    void testGetListAccountsMarkerWithExistingCache2() {

        final CacheFactory cacheFactoryMock = Mockito.mock(CacheFactory.class);
        final CacheImpl<Integer, String> cache = new CacheImpl<>(DEFAULT_CACHE_MAX_CAPACITY);
        cache.put(1000,"M1000");
        cache.put(4000,"M4000");
        when(cacheFactoryMock.getCache(NAME_LIST_ACCOUNTS_CACHE)).thenReturn(cache);

        ReflectionTestUtils.setField(vaultAdminImpl, CACHE_FACTORY, cacheFactoryMock);
        vaultAdminImpl.initCaches();

        final String marker = vaultAdminImpl.getAccountsMarker(500, CD_TENANT_ID_PREFIX);
        assertEquals("M500", marker);
    }

    @Test
    void testListAccountsOffsetEmptyCache() {

        final CacheFactory cacheFactoryMock = Mockito.mock(CacheFactory.class);
        when(cacheFactoryMock.getCache(NAME_LIST_ACCOUNTS_CACHE)).thenReturn(new CacheImpl<>(DEFAULT_CACHE_MAX_CAPACITY));

        ReflectionTestUtils.setField(vaultAdminImpl, CACHE_FACTORY, cacheFactoryMock);
        vaultAdminImpl.initCaches();

        final ListAccountsRequestDTO   listAccountsRequestDTO = ListAccountsRequestDTO.builder()
                .maxItems(1000)
                .build();

        final ListAccountsResponseDTO response = vaultAdminImpl.listAccounts(3000, listAccountsRequestDTO);
        assertEquals(1000, response.getAccounts().size());
        assertEquals(response.getAccounts().get(0).getCustomAttributes(), null);
    }

    @Test
    void testListAccountsOffsetWithCache1() {

        final CacheFactory cacheFactoryMock = Mockito.mock(CacheFactory.class);
        final CacheImpl<Integer, String> cache = new CacheImpl<>(DEFAULT_CACHE_MAX_CAPACITY);
        cache.put(1000,"M1000");
        cache.put(2000,"M2000");
        when(cacheFactoryMock.getCache(NAME_LIST_ACCOUNTS_CACHE)).thenReturn(cache);

        ReflectionTestUtils.setField(vaultAdminImpl, CACHE_FACTORY, cacheFactoryMock);
        vaultAdminImpl.initCaches();

        final ListAccountsRequestDTO   listAccountsRequestDTO = ListAccountsRequestDTO.builder()
                .maxItems(1000)
                .build();

        final ListAccountsResponseDTO response = vaultAdminImpl.listAccounts(1000, listAccountsRequestDTO);
        assertEquals(1000, response.getAccounts().size());
        assertEquals("M2000", response.getMarker());
        assertEquals(response.getAccounts().get(0).getCustomAttributes(), null);
    }

    @Test
    void testListAccountsOffsetWithCache2() {

        final CacheFactory cacheFactoryMock = Mockito.mock(CacheFactory.class);
        final CacheImpl<Integer, String> cache = new CacheImpl<>(DEFAULT_CACHE_MAX_CAPACITY);
        cache.put(1000,"M1000");
        cache.put(4000,"M4000");
        when(cacheFactoryMock.getCache(NAME_LIST_ACCOUNTS_CACHE)).thenReturn(cache);

        ReflectionTestUtils.setField(vaultAdminImpl, CACHE_FACTORY, cacheFactoryMock);
        vaultAdminImpl.initCaches();

        final ListAccountsRequestDTO   listAccountsRequestDTO = ListAccountsRequestDTO.builder()
                .maxItems(1000)
                .build();

        final ListAccountsResponseDTO response = vaultAdminImpl.listAccounts(3000, listAccountsRequestDTO);
        assertEquals(1000, response.getAccounts().size());
        assertEquals("M4000", response.getMarker());
        assertEquals(response.getAccounts().get(0).getCustomAttributes(), null);
    }

    @Test
    void testGetTempAccountCredentialsEmptyCache() {

        final CacheFactory cacheFactoryMock = Mockito.mock(CacheFactory.class);
        when(cacheFactoryMock.getCache(NAME_ASSUME_ROLE_CACHE)).thenReturn(new CacheImpl<>(DEFAULT_CACHE_MAX_CAPACITY));

        ReflectionTestUtils.setField(vaultAdminImpl, CACHE_FACTORY, cacheFactoryMock);
        vaultAdminImpl.initCaches();

        final AssumeRoleRequest assumeRoleRequest = new AssumeRoleRequest();
        assumeRoleRequest.setRoleArn(TEST_ROLE_ARN);
        assumeRoleRequest.setRoleSessionName(TEST_SESSION_NAME);

        final Credentials response = vaultAdminImpl.getTempAccountCredentials(assumeRoleRequest);
        assertEquals(TEST_ACCESS_KEY, response.getAccessKeyId());
        assertEquals(TEST_SECRET_KEY, response.getSecretAccessKey());
        assertEquals(TEST_SESSION_TOKEN, response.getSessionToken());
    }

    @Test
    void testGetTempAccountCredentialsWithCache() {

        final CacheFactory cacheFactoryMock = Mockito.mock(CacheFactory.class);

        final CacheImpl<String, Credentials> cache = new CacheImpl<>(DEFAULT_CACHE_MAX_CAPACITY);

        final Credentials credentials = new Credentials();
        credentials.setAccessKeyId(TEST_ACCESS_KEY);
        credentials.setSecretAccessKey(TEST_SECRET_KEY);
        credentials.setExpiration(new Date());
        credentials.setSessionToken(TEST_SESSION_TOKEN);
        cache.put(TEST_ROLE_ARN, credentials);

        when(cacheFactoryMock.getCache(NAME_ASSUME_ROLE_CACHE)).thenReturn(cache);

        ReflectionTestUtils.setField(vaultAdminImpl, CACHE_FACTORY, cacheFactoryMock);
        vaultAdminImpl.initCaches();

        final AssumeRoleRequest assumeRoleRequest = new AssumeRoleRequest()
                                                        .withRoleArn(TEST_ROLE_ARN)
                                                        .withRoleSessionName(TEST_SESSION_NAME);

        final Credentials response = vaultAdminImpl.getTempAccountCredentials(assumeRoleRequest);
        assertEquals(TEST_ACCESS_KEY, response.getAccessKeyId());
        assertEquals(TEST_SECRET_KEY, response.getSecretAccessKey());
        assertEquals(TEST_SESSION_TOKEN, response.getSessionToken());
    }

    @Test
    void assumeRoleBackbeat400ServiceResponse() {

        final AssumeRoleRequest assumeRoleRequest = new AssumeRoleRequest()
                .withRoleArn(TEST_ROLE_ARN)
                .withRoleSessionName(TEST_SESSION_NAME);

        when(stsClient.assumeRoleBackbeat(any(AssumeRoleRequest.class)))
                .thenAnswer((Answer<Response<AssumeRoleResult>>) invocation -> {
                    final HttpResponse   httpResponse = new HttpResponse(null, null);
                    httpResponse.setStatusCode(400);
                    httpResponse.setStatusText("WrongFormat");
                    return new Response<>(null,httpResponse);
                });

        final VaultServiceException exception = assertThrows(VaultServiceException.class, () -> {
            vaultAdminImpl.getTempAccountCredentials(assumeRoleRequest);
        });
        assertEquals(400, exception.getStatus().value());
        assertEquals("WrongFormat", exception.getReason());

        //reinit the default mocks
        initMocks();
    }

    @Test
    void assumeRoleBackbeat500ServiceResponse() {

        final AssumeRoleRequest assumeRoleRequest = new AssumeRoleRequest()
                .withRoleArn(TEST_ROLE_ARN)
                .withRoleSessionName(TEST_SESSION_NAME);

        when(stsClient.assumeRoleBackbeat(any(AssumeRoleRequest.class)))
                .thenAnswer((Answer<Response<AssumeRoleResult>>) invocation -> {
                    final HttpResponse   httpResponse = new HttpResponse(null, null);
                    httpResponse.setStatusCode(500);
                    httpResponse.setStatusText("ServiceFailure");
                    return new Response<>(null,httpResponse);
                });

        final VaultServiceException exception = assertThrows(VaultServiceException.class, () -> {
            vaultAdminImpl.getTempAccountCredentials(assumeRoleRequest);
        });
        assertEquals(500, exception.getStatus().value());
        assertEquals("ServiceFailure", exception.getReason());

        //reinit the default mocks
        initMocks();
    }

    @Test
    void testVaultAdminImplInvalidS3InterfaceEndpoint() {
        assertThrows(IllegalArgumentException.class,
                () -> new VaultAdminImpl(accountServicesClient, stsClient, vaultAdminEndpoint, "dummy_endpoint"),
                "VaultAdminImpl constructor should throw IllegalArgumentException for null endpoint");
    }

    @Test
    void testGetIAMClient() {
        final Credentials credentials = new Credentials();
        credentials.setAccessKeyId(TEST_ACCESS_KEY);
        credentials.setSecretAccessKey(TEST_SECRET_KEY);

        final AmazonIdentityManagement iam = vaultAdminImpl.getIAMClient(credentials, TEST_REGION);
        assertNotNull(iam);
    }

    @Test
    void testGetIAMClientWithSession() {
        final Credentials credentials = new Credentials();
        credentials.setAccessKeyId(TEST_ACCESS_KEY);
        credentials.setSecretAccessKey(TEST_SECRET_KEY);
        credentials.setSessionToken(TEST_SESSION_TOKEN);

        final AmazonIdentityManagement iam = vaultAdminImpl.getIAMClient(credentials, TEST_REGION);
        assertNotNull(iam);
    }

    @Test
    void testGetAccountIDEmptyCache() {

        final CacheFactory cacheFactoryMock = Mockito.mock(CacheFactory.class);
        when(cacheFactoryMock.getCache(NAME_ACCOUNT_ID_CACHE)).thenReturn(new CacheImpl<>(DEFAULT_CACHE_MAX_CAPACITY));

        ReflectionTestUtils.setField(vaultAdminImpl, CACHE_FACTORY, cacheFactoryMock);
        vaultAdminImpl.initCaches();

        final ListAccountsRequestDTO   listAccountsRequestDTO = ListAccountsRequestDTO.builder()
                .maxItems(1000)
                .filterKey(CD_TENANT_ID_PREFIX + UUID.randomUUID())
                .build();

        final String response = vaultAdminImpl.getAccountID(listAccountsRequestDTO);
        assertTrue(response.startsWith(DEFAULT_TEST_ACCOUNT_ID));
    }

    @Test
    void testGetAccountIDWithCache() {

        final String cdTenantIDFilter1 = CD_TENANT_ID_PREFIX + UUID.randomUUID();
        final String cdTenantIDFilter2 = CD_TENANT_ID_PREFIX + UUID.randomUUID();

        final CacheFactory cacheFactoryMock = Mockito.mock(CacheFactory.class);
        final CacheImpl<String, String> cache = new CacheImpl<>(DEFAULT_CACHE_MAX_CAPACITY);
        cache.put(cdTenantIDFilter1,DEFAULT_TEST_ACCOUNT_ID + "TEST1");
        cache.put(cdTenantIDFilter2,DEFAULT_TEST_ACCOUNT_ID + "TEST2");

        when(cacheFactoryMock.getCache(NAME_ACCOUNT_ID_CACHE)).thenReturn(cache);

        ReflectionTestUtils.setField(vaultAdminImpl, CACHE_FACTORY, cacheFactoryMock);
        vaultAdminImpl.initCaches();

        final ListAccountsRequestDTO   listAccountsRequestDTO = ListAccountsRequestDTO.builder()
                .maxItems(1000)
                .filterKey(cdTenantIDFilter1)
                .build();

        final String response = vaultAdminImpl.getAccountID(listAccountsRequestDTO);
        assertEquals(DEFAULT_TEST_ACCOUNT_ID + "TEST1", response);
    }

    @Test
    void testGetAccountIDNotFound() {

        final CacheFactory cacheFactoryMock = Mockito.mock(CacheFactory.class);
        when(cacheFactoryMock.getCache(NAME_ACCOUNT_ID_CACHE)).thenReturn(new CacheImpl<>(DEFAULT_CACHE_MAX_CAPACITY));

        ReflectionTestUtils.setField(vaultAdminImpl, CACHE_FACTORY, cacheFactoryMock);
        vaultAdminImpl.initCaches();

        when(accountServicesClient.listAccounts(any(ListAccountsRequestDTO.class)))
                .thenAnswer((Answer<Response<ListAccountsResponseDTO>>) invocation -> {
                    final ListAccountsResponseDTO response = new ListAccountsResponseDTO();
                    response.setAccounts(new ArrayList<>());

                    final HttpResponse httpResponse = new HttpResponse(null, null);
                    httpResponse.setStatusCode(200);
                    httpResponse.setStatusText("OK");
                    return new Response<>(response,httpResponse);
                });

        final ListAccountsRequestDTO   listAccountsRequestDTO = ListAccountsRequestDTO.builder()
                .maxItems(1000)
                .filterKey(CD_TENANT_ID_PREFIX + UUID.randomUUID())
                .build();

        assertThrows(VaultServiceException.class, () -> vaultAdminImpl.getAccountID(listAccountsRequestDTO));
    }

    @Test
    void updateAccountAttributes() {

        final String name = "tenant.name";
        final Map<String, String> customAttributestemp  = new HashMap<>() ;
        customAttributestemp.put("cd_tenant_id==" + UUID.randomUUID(), "");

        final UpdateAccountAttributesRequestDTO updateAccountAttributesRequestDTO = UpdateAccountAttributesRequestDTO.builder()
                .name(name)
                .customAttributes(customAttributestemp)
                .build();

        final CreateAccountResponseDTO response = vaultAdminImpl.updateAccountAttributes(updateAccountAttributesRequestDTO);
        assertEquals(name, response.getAccount().getData().getName());
        assertNotNull(response.getAccount().getData().getArn());
        assertNotNull(response.getAccount().getData().getCreateDate());
        assertNotNull(response.getAccount().getData().getId());
        assertNotNull(response.getAccount().getData().getCanonicalId());
        assertNotNull(response.getAccount().getData().getCustomAttributes());
    }
}
