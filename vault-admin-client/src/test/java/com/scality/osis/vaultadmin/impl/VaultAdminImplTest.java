package com.scality.osis.vaultadmin.impl;

import com.amazonaws.Response;
import com.amazonaws.http.HttpResponse;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.scality.osis.vaultadmin.impl.cache.CacheFactory;
import com.scality.osis.vaultadmin.impl.cache.CacheImpl;
import com.scality.vaultclient.dto.AssumeRoleResult;
import com.scality.vaultclient.dto.CreateAccountRequestDTO;
import com.scality.vaultclient.dto.CreateAccountResponseDTO;
import com.scality.vaultclient.dto.ListAccountsRequestDTO;
import com.scality.vaultclient.dto.ListAccountsResponseDTO;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static com.scality.osis.vaultadmin.impl.cache.CacheConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SuppressWarnings("ConstantConditions")
public class VaultAdminImplTest extends BaseTest {


    public static final String CD_TENANT_ID_PREFIX = "cd_tenant_id";
    @Test
    public void createAccount() {

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
    public void createAccountErrorExistingEntity() {

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
        assertEquals(409, exception.status());
        assertEquals("EntityAlreadyExists", exception.getMessage());

        //reinit the default mocks
        initMocks();
    }

    @Test
    public void createAccountIOException() {

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
        assertEquals(500, exception.status());
        assertEquals("Exception", exception.getMessage());

        //reinit the default mocks
        initMocks();
    }

    @Test
    public void createAccountErrServiceResponse() {

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
        assertEquals(403, exception.status());
        assertEquals("Forbidden", exception.getMessage());

        //reinit the default mocks
        initMocks();
    }

    @Test
    public void listAccounts() {

        final ListAccountsRequestDTO   listAccountsRequestDTO = ListAccountsRequestDTO.builder()
                .maxItems(5)
                .filterKeyStartsWith(CD_TENANT_ID_PREFIX)
                .build();

        final ListAccountsResponseDTO response = vaultAdminImpl.listAccounts(listAccountsRequestDTO);
        assertNotEquals(0, response.getAccounts().size());
        assertFalse(response.getAccounts().get(0).getCustomAttributes().isEmpty());
        assertTrue(response.getAccounts().get(0)
                .getCustomAttributes().keySet()
                .stream().findFirst().get()
                .startsWith(CD_TENANT_ID_PREFIX));
    }

    @Test
    public void listAccounts400ServiceResponse() {

        final ListAccountsRequestDTO   listAccountsRequestDTO = ListAccountsRequestDTO.builder()
                .maxItems(5)
                .filterKeyStartsWith(CD_TENANT_ID_PREFIX)
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
        assertEquals(400, exception.status());
        assertEquals("WrongFormat", exception.getMessage());

        //reinit the default mocks
        initMocks();
    }

    @Test
    public void listAccounts500ServiceResponse() {

        final ListAccountsRequestDTO   listAccountsRequestDTO = ListAccountsRequestDTO.builder()
                .maxItems(5)
                .filterKeyStartsWith(CD_TENANT_ID_PREFIX)
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
        assertEquals(500, exception.status());
        assertEquals("ServiceFailure", exception.getMessage());

        //reinit the default mocks
        initMocks();
    }

    @Test
    public void testVaultAdminImplInvalidEndpoint() {
        assertThrows(IllegalArgumentException.class,
                () -> new VaultAdminImpl(accountServicesClient, stsClient, "dummy_vault_admin_endpoint", s3InterfaceEndpoint),
                "VaultAdminImpl constructor should throw IllegalArgumentException for null endpoint");
    }

    @Test
    public void testGetListAccountsMarkerCacheEmpty() {

        final CacheFactory cacheFactoryMock = Mockito.mock(CacheFactory.class);
        when(cacheFactoryMock.getCache(NAME_LIST_ACCOUNTS_CACHE)).thenReturn(new CacheImpl<>(DEFAULT_CACHE_MAX_CAPACITY));

        ReflectionTestUtils.setField(vaultAdminImpl, "cacheFactory", cacheFactoryMock);
        vaultAdminImpl.initCaches();

        final String marker = vaultAdminImpl.getAccountsMarker(1000, CD_TENANT_ID_PREFIX);
        assertEquals("M1000", marker);
    }

    @Test
    public void testGetListAccountsMarkerWithExistingCache1() {

        final CacheFactory cacheFactoryMock = Mockito.mock(CacheFactory.class);
        final CacheImpl<Integer, String> cache = new CacheImpl<>(DEFAULT_CACHE_MAX_CAPACITY);
        cache.put(1000,"M1000");
        cache.put(2000,"M2000");
        cache.put(4000,"M4000");
        when(cacheFactoryMock.getCache(NAME_LIST_ACCOUNTS_CACHE)).thenReturn(cache);

        ReflectionTestUtils.setField(vaultAdminImpl, "cacheFactory", cacheFactoryMock);
        vaultAdminImpl.initCaches();

        final String marker = vaultAdminImpl.getAccountsMarker(3000, CD_TENANT_ID_PREFIX);
        assertEquals("M3000", marker);
    }

    @Test
    public void testGetListAccountsMarkerWithExistingCache2() {

        final CacheFactory cacheFactoryMock = Mockito.mock(CacheFactory.class);
        final CacheImpl<Integer, String> cache = new CacheImpl<>(DEFAULT_CACHE_MAX_CAPACITY);
        cache.put(1000,"M1000");
        cache.put(4000,"M4000");
        when(cacheFactoryMock.getCache(NAME_LIST_ACCOUNTS_CACHE)).thenReturn(cache);

        ReflectionTestUtils.setField(vaultAdminImpl, "cacheFactory", cacheFactoryMock);
        vaultAdminImpl.initCaches();

        final String marker = vaultAdminImpl.getAccountsMarker(500, CD_TENANT_ID_PREFIX);
        assertEquals("M500", marker);
    }

    @Test
    public void testListAccountsOffsetEmptyCache() {

        final CacheFactory cacheFactoryMock = Mockito.mock(CacheFactory.class);
        when(cacheFactoryMock.getCache(NAME_LIST_ACCOUNTS_CACHE)).thenReturn(new CacheImpl<>(DEFAULT_CACHE_MAX_CAPACITY));

        ReflectionTestUtils.setField(vaultAdminImpl, "cacheFactory", cacheFactoryMock);
        vaultAdminImpl.initCaches();

        final ListAccountsRequestDTO   listAccountsRequestDTO = ListAccountsRequestDTO.builder()
                .maxItems(1000)
                .filterKeyStartsWith(CD_TENANT_ID_PREFIX)
                .build();

        final ListAccountsResponseDTO response = vaultAdminImpl.listAccounts(3000, listAccountsRequestDTO);
        assertEquals(1000, response.getAccounts().size());
        assertFalse(response.getAccounts().get(0).getCustomAttributes().isEmpty());
        assertTrue(response.getAccounts().get(0)
                .getCustomAttributes().keySet()
                .stream().findFirst().get()
                .startsWith(CD_TENANT_ID_PREFIX));
    }

    @Test
    public void testListAccountsOffsetWithCache1() {

        final CacheFactory cacheFactoryMock = Mockito.mock(CacheFactory.class);
        final CacheImpl<Integer, String> cache = new CacheImpl<>(DEFAULT_CACHE_MAX_CAPACITY);
        cache.put(1000,"M1000");
        cache.put(2000,"M2000");
        when(cacheFactoryMock.getCache(NAME_LIST_ACCOUNTS_CACHE)).thenReturn(cache);

        ReflectionTestUtils.setField(vaultAdminImpl, "cacheFactory", cacheFactoryMock);
        vaultAdminImpl.initCaches();

        final ListAccountsRequestDTO   listAccountsRequestDTO = ListAccountsRequestDTO.builder()
                .maxItems(1000)
                .filterKeyStartsWith(CD_TENANT_ID_PREFIX)
                .build();

        final ListAccountsResponseDTO response = vaultAdminImpl.listAccounts(1000, listAccountsRequestDTO);
        assertEquals(1000, response.getAccounts().size());
        assertEquals("M2000", response.getMarker());
        assertFalse(response.getAccounts().get(0).getCustomAttributes().isEmpty());
        assertTrue(response.getAccounts().get(0)
                .getCustomAttributes().keySet()
                .stream().findFirst().get()
                .startsWith(CD_TENANT_ID_PREFIX));
    }

    @Test
    public void testListAccountsOffsetWithCache2() {

        final CacheFactory cacheFactoryMock = Mockito.mock(CacheFactory.class);
        final CacheImpl<Integer, String> cache = new CacheImpl<>(DEFAULT_CACHE_MAX_CAPACITY);
        cache.put(1000,"M1000");
        cache.put(4000,"M4000");
        when(cacheFactoryMock.getCache(NAME_LIST_ACCOUNTS_CACHE)).thenReturn(cache);

        ReflectionTestUtils.setField(vaultAdminImpl, "cacheFactory", cacheFactoryMock);
        vaultAdminImpl.initCaches();

        final ListAccountsRequestDTO   listAccountsRequestDTO = ListAccountsRequestDTO.builder()
                .maxItems(1000)
                .filterKeyStartsWith(CD_TENANT_ID_PREFIX)
                .build();

        final ListAccountsResponseDTO response = vaultAdminImpl.listAccounts(3000, listAccountsRequestDTO);
        assertEquals(1000, response.getAccounts().size());
        assertEquals("M4000", response.getMarker());
        assertFalse(response.getAccounts().get(0).getCustomAttributes().isEmpty());
        assertTrue(response.getAccounts().get(0)
                .getCustomAttributes().keySet()
                .stream().findFirst().get()
                .startsWith(CD_TENANT_ID_PREFIX));
    }

    @Test
    public void testGetTempAccountCredentialsEmptyCache() {

        final CacheFactory cacheFactoryMock = Mockito.mock(CacheFactory.class);
        when(cacheFactoryMock.getCache(NAME_ASSUME_ROLE_CACHE)).thenReturn(new CacheImpl<>(DEFAULT_CACHE_MAX_CAPACITY));

        ReflectionTestUtils.setField(vaultAdminImpl, "cacheFactory", cacheFactoryMock);
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
    public void testGetTempAccountCredentialsWithCache() {

        final CacheFactory cacheFactoryMock = Mockito.mock(CacheFactory.class);

        final CacheImpl<String, Credentials> cache = new CacheImpl<>(DEFAULT_CACHE_MAX_CAPACITY);

        final Credentials credentials = new Credentials();
        credentials.setAccessKeyId(TEST_ACCESS_KEY);
        credentials.setSecretAccessKey(TEST_SECRET_KEY);
        credentials.setExpiration(new Date());
        credentials.setSessionToken(TEST_SESSION_TOKEN);
        cache.put(TEST_ROLE_ARN, credentials);

        when(cacheFactoryMock.getCache(NAME_ASSUME_ROLE_CACHE)).thenReturn(cache);

        ReflectionTestUtils.setField(vaultAdminImpl, "cacheFactory", cacheFactoryMock);
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
    public void assumeRoleBackbeat400ServiceResponse() {

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
        assertEquals(400, exception.status());
        assertEquals("WrongFormat", exception.getMessage());

        //reinit the default mocks
        initMocks();
    }

    @Test
    public void assumeRoleBackbeat500ServiceResponse() {

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
        assertEquals(500, exception.status());
        assertEquals("ServiceFailure", exception.getMessage());

        //reinit the default mocks
        initMocks();
    }

    @Test
    public void testVaultAdminImplInvalidS3InterfaceEndpoint() {
        assertThrows(IllegalArgumentException.class,
                () -> new VaultAdminImpl(accountServicesClient, stsClient, vaultAdminEndpoint, "dummy_endpoint"),
                "VaultAdminImpl constructor should throw IllegalArgumentException for null endpoint");
    }
}
