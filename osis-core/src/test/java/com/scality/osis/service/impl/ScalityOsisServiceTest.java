package com.scality.osis.service.impl;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.*;
import com.amazonaws.services.securitytoken.model.*;
import com.scality.osis.ScalityAppEnv;
import com.scality.osis.vaultadmin.impl.cache.*;
import com.vmware.osis.model.*;
import com.vmware.osis.model.exception.*;
import com.vmware.osis.resource.OsisCapsManager;
import org.junit.jupiter.api.BeforeEach;
import com.scality.vaultclient.dto.*;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import com.scality.osis.vaultadmin.impl.*;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static com.scality.osis.utils.ScalityConstants.*;
import static com.scality.osis.utils.ScalityTestUtils.*;
import static com.scality.osis.vaultadmin.impl.cache.CacheConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ScalityOsisServiceTest {

    //vault admin mock object
    @Mock
    private static VaultAdminImpl vaultAdminMock;

    private static ScalityOsisService scalityOsisServiceUnderTest;

    private static AsyncScalityOsisService asyncScalityOsisServiceUnderTest;

    @Mock
    private static ScalityAppEnv appEnvMock;

    @Mock
    private static OsisCapsManager osisCapsManagerMock;

    @Mock
    private AmazonIdentityManagement iamMock;

    @BeforeEach
    private void init(){
        MockitoAnnotations.initMocks( this );
        initMocks();
        scalityOsisServiceUnderTest = new ScalityOsisService(appEnvMock, vaultAdminMock, osisCapsManagerMock);

        asyncScalityOsisServiceUnderTest = new AsyncScalityOsisService();
        ReflectionTestUtils.setField(asyncScalityOsisServiceUnderTest, "vaultAdmin", vaultAdminMock);
        ReflectionTestUtils.setField(asyncScalityOsisServiceUnderTest, "appEnv", appEnvMock);

        ReflectionTestUtils.setField(scalityOsisServiceUnderTest, "asyncScalityOsisService", asyncScalityOsisServiceUnderTest);
    }

    private void initMocks() {
        when(appEnvMock.getConsoleEndpoint()).thenReturn(TEST_CONSOLE_URL);
        when(appEnvMock.isApiTokenEnabled()).thenReturn(false);
        when(appEnvMock.getStorageInfo()).thenReturn(Collections.singletonList("standard"));
        when(appEnvMock.getRegionInfo()).thenReturn(Collections.singletonList("default"));
        when(appEnvMock.getPlatformName()).thenReturn(PLATFORM_NAME);
        when(appEnvMock.getPlatformVersion()).thenReturn(PLATFORM_VERSION);
        when(appEnvMock.getApiVersion()).thenReturn(API_VERSION);
        when(appEnvMock.getS3InterfaceEndpoint()).thenReturn(TEST_S3_INTERFACE_URL);
        when(appEnvMock.getAssumeRoleName()).thenReturn(SAMPLE_ASSUME_ROLE_NAME);
        when(osisCapsManagerMock.getNotImplements()).thenReturn(new ArrayList<>());
        when(vaultAdminMock.getIAMClient(any(Credentials.class),any())).thenReturn(iamMock);

        initCreateTenantMocks();
        initListTenantMocks();
        initTempCredentialsMocks();
        initCreatePolicyMocks();
        initGenerateAccountAKMocks();
        initCreateRoleMocks();
        initAttachRolePolicyMocks();
        initDeleteAccessKeyMocks();
        initGetAccountMocks();
        initCaches();
    }

    private void initCreateTenantMocks() {
        //initialize mock create account response
        when(vaultAdminMock.createAccount(any(CreateAccountRequestDTO.class)))
                .thenAnswer((Answer<CreateAccountResponseDTO>) invocation -> {
                    final CreateAccountRequestDTO request = invocation.getArgument(0);
                    final AccountData data = new AccountData();
                    data.setEmailAddress(request.getEmailAddress());
                    data.setName(request.getName());
                    if(StringUtils.isEmpty(request.getExternalAccountId())){
                        data.setId(SAMPLE_TENANT_ID);
                    } else{
                        data.setId(request.getExternalAccountId());
                    }
                    data.setCustomAttributes(request.getCustomAttributes());
                    final Account account = new Account();
                    account.setData(data);
                    final CreateAccountResponseDTO response = new CreateAccountResponseDTO();
                    response.setAccount(account);

                    return response;
                });
    }

    private void initListTenantMocks() {

        //initialize mock list accounts response
        when(vaultAdminMock.listAccounts(anyLong(),any(ListAccountsRequestDTO.class)))
                .thenAnswer((Answer<ListAccountsResponseDTO>) invocation -> {
                    final long offset = invocation.getArgument(0);
                    final ListAccountsRequestDTO request = invocation.getArgument(1);
                    final int maxItems = request.getMaxItems();
                    final List<AccountData> accounts = new ArrayList<>();

                    // Generate Accounts with ids (markerVal + i) to maxItems count
                    for( int index = 0; index < maxItems; index++){
                        final AccountData data = new AccountData();
                        data.setEmailAddress("xyz@scality.com");
                        data.setName(TEST_NAME);
                        data.setId(SAMPLE_TENANT_ID + (index + offset)); //setting ID with index

                        // if filterStartsWith generate customAttributes for all accounts
                        final Map<String, String> customAttributestemp  = new HashMap<>() ;
                        data.setCustomAttributes(customAttributestemp);
                        accounts.add(data);

                        if(!StringUtils.isEmpty(request.getFilterKey()) && request.getFilterKey().startsWith(CD_TENANT_ID_PREFIX)) {
                            // If filter key exists mock only one account in the response with filterKey as customAttributestemp
                            customAttributestemp.put(request.getFilterKey(), "");
                            break;
                        } else {
                            customAttributestemp.put(CD_TENANT_ID_PREFIX + UUID.randomUUID(), "");
                        }
                    }

                    final ListAccountsResponseDTO response = new ListAccountsResponseDTO();
                    response.setAccounts(accounts);
                    response.setMarker("M"+(offset+maxItems));
                    response.setTruncated(true);
                    return response;
                });
    }

    private void initTempCredentialsMocks() {
        //initialize mock vault admin temporary credentials response
        when(vaultAdminMock.getTempAccountCredentials(any(AssumeRoleRequest.class)))
                .thenAnswer((Answer<Credentials>) invocation -> {
                    final Credentials credentials = new Credentials();
                    credentials.setAccessKeyId(TEST_ACCESS_KEY);
                    credentials.setSecretAccessKey(TEST_SECRET_KEY);
                    credentials.setExpiration(new Date());
                    credentials.setSessionToken(TEST_SESSION_TOKEN);

                    return credentials;
                });
    }

    private void initGenerateAccountAKMocks() {
        when(vaultAdminMock.getAccountAccessKey(any(GenerateAccountAccessKeyRequest.class)))
                .thenAnswer((Answer<GenerateAccountAccessKeyResponse>) invocation -> {
                    final GenerateAccountAccessKeyRequest request = invocation.getArgument(0);

                    final Date expirationDate = new Date(new Date().getTime() + (request.getDurationSeconds() * 1000L));
                    final AccountSecretKeyData accountSecretKeyData = new AccountSecretKeyData().builder()
                            .id(TEST_ACCESS_KEY)
                            .value(TEST_SECRET_KEY)
                            .userId(SAMPLE_TENANT_ID)
                            .createDate(new Date())
                            .lastUsedService(NA_STR)
                            .lastUsedRegion(NA_STR)
                            .lastUsedDate(new Date())
                            .status(ACTIVE_STR)
                            .notAfter(expirationDate)
                            .build();
                    final GenerateAccountAccessKeyResponse response = new GenerateAccountAccessKeyResponse();
                    response.setData(accountSecretKeyData);

                    return response;
                });
    }

    private void initCreateRoleMocks() {
        when(iamMock.createRole(any(CreateRoleRequest.class)))
                .thenAnswer((Answer<CreateRoleResult>) invocation -> {
                    final CreateRoleRequest request = invocation.getArgument(0);
                    final CreateRoleResult response = new CreateRoleResult();
                    final Role role = new Role()
                            .withAssumeRolePolicyDocument(request.getAssumeRolePolicyDocument())
                            .withDescription(request.getDescription())
                            .withRoleName(request.getRoleName())
                            .withCreateDate(new Date())
                            .withRoleId(SAMPLE_ID)
                            .withArn("arn:aws:iam::" + TEST_TENANT_ID +":role/" + request.getRoleName());
                    response.setRole(role);
                    return response;
                });
    }

    private void initAttachRolePolicyMocks() {
        when(iamMock.attachRolePolicy(any(AttachRolePolicyRequest.class))).thenReturn(new AttachRolePolicyResult());
    }

    private void initDeleteAccessKeyMocks() {
        when(iamMock.deleteAccessKey(any(DeleteAccessKeyRequest.class))).thenReturn( new DeleteAccessKeyResult());
    }

    private void initCreatePolicyMocks() {
        when(iamMock.createPolicy(any(CreatePolicyRequest.class)))
                .thenAnswer((Answer<CreatePolicyResult>) invocation -> {
                    final CreatePolicyRequest request = invocation.getArgument(0);
                    final CreatePolicyResult response = new CreatePolicyResult();
                    final Policy policy = new Policy()
                            .withPolicyName(request.getPolicyName())
                            .withDescription(request.getDescription())
                            .withArn("arn:aws:iam::" + TEST_TENANT_ID +":policy/" + request.getPolicyName());
                    response.setPolicy(policy);
                    return response;
                });
    }

    private void initGetAccountMocks() {
        when(vaultAdminMock.getAccountWithID(any(GetAccountRequestDTO.class)))
                .thenAnswer((Answer<AccountData>) invocation -> {

                    final GetAccountRequestDTO request = invocation.getArgument(0);
                    final String accountId = request.getAccountId() ==null ? SAMPLE_TENANT_ID : request.getAccountId();
                    final String accountName = request.getAccountName() ==null ? SAMPLE_TENANT_NAME : request.getAccountName();
                    final String accountArn = request.getAccountArn();
                    final String emailAddress = request.getEmailAddress() ==null ? SAMPLE_SCALITY_ACCOUNT_EMAIL : request.getEmailAddress();
                    final String canonicalId = request.getCanonicalId() ==null ? SAMPLE_ID : request.getCanonicalId();
                    final Map<String, String> customAttributestes  = new HashMap<>() ;
                    customAttributestes.put(CD_TENANT_ID_PREFIX + UUID.randomUUID(), "");

                    final AccountData data = new AccountData();
                    data.setEmailAddress(emailAddress);
                    data.setName(accountName);
                    data.setArn(accountArn);
                    data.setCreateDate(new Date());
                    data.setId(accountId);
                    data.setCanonicalId(canonicalId);
                    data.setCustomAttributes(customAttributestes);

                    return data;
                });
    }

    private void initCaches() {
        final CacheFactory cacheFactoryMock = mock(CacheFactory.class);
        when(cacheFactoryMock.getCache(NAME_LIST_ACCOUNTS_CACHE)).thenReturn(new CacheImpl<>(DEFAULT_CACHE_MAX_CAPACITY));

        ReflectionTestUtils.setField(vaultAdminMock, "cacheFactory", cacheFactoryMock);
        vaultAdminMock.initCaches();
    }

    @Test
    public void testCreateTenant(){

        // Call Scality Osis service to create a tenant
        final OsisTenant osisTenantRes = scalityOsisServiceUnderTest.createTenant(createSampleOsisTenantObj());

        assertEquals(SAMPLE_ID, osisTenantRes.getTenantId());
        assertEquals(SAMPLE_TENANT_NAME, osisTenantRes.getName());
        assertTrue(SAMPLE_CD_TENANT_IDS.size() == osisTenantRes.getCdTenantIds().size() &&
                SAMPLE_CD_TENANT_IDS.containsAll(osisTenantRes.getCdTenantIds()) && osisTenantRes.getCdTenantIds().containsAll(SAMPLE_CD_TENANT_IDS));
        assertTrue(osisTenantRes.getActive());
    }

    @Test
    public void testCreateTenantInactive(){
        final OsisTenant osisTenantReq = createSampleOsisTenantObj();
        osisTenantReq.active(false);

        // Call Scality Osis service to create a tenant
        assertThrows(BadRequestException.class, () -> {
            scalityOsisServiceUnderTest.createTenant(osisTenantReq);
        });
    }

    @Test
    public void testCreateTenant409(){

        when(vaultAdminMock.createAccount(any(CreateAccountRequestDTO.class)))
                .thenAnswer((Answer<CreateAccountResponseDTO>) invocation -> {
                    throw new VaultServiceException(HttpStatus.CONFLICT, "EntityAlreadyExists");
                });

        assertThrows(VaultServiceException.class, () -> {
            scalityOsisServiceUnderTest.createTenant(createSampleOsisTenantObj());
        });

        //resetting mocks to original
        init();
    }

    @Test
    public void testCreateTenant400(){

        when(vaultAdminMock.createAccount(any(CreateAccountRequestDTO.class)))
                .thenAnswer((Answer<CreateAccountResponseDTO>) invocation -> {
                    throw new VaultServiceException(HttpStatus.BAD_REQUEST, "Bad Request");
                });

        assertThrows(VaultServiceException.class, () -> {
            scalityOsisServiceUnderTest.createTenant(createSampleOsisTenantObj());
        });

        //resetting mocks to original
        initMocks();
    }

    @Test
    public void testListTenants() {
        // Setup
        final long offset = 0L;
        final long limit = 1000L;
        // Run the test
        // Call Scality Osis service to list tenants
        final PageOfTenants response = scalityOsisServiceUnderTest.listTenants(offset, limit);

        // Verify the results
        assertEquals(limit, response.getPageInfo().getTotal());
        assertEquals(offset, response.getPageInfo().getOffset());
        assertEquals(limit, response.getPageInfo().getLimit());
        assertEquals((int)limit, response.getItems().size());
        assertNotNull(response.getItems().get(0).getCdTenantIds());
    }

    @Test
    public void testListTenantsOffset() {
        // Setup
        final long offset = 2000L;
        final long limit = 1000L;
        // Run the test
        // Call Scality Osis service to list tenants
        final PageOfTenants response = scalityOsisServiceUnderTest.listTenants(offset, limit);


        // Verify the results
        assertEquals(limit, response.getPageInfo().getTotal());
        assertEquals(offset, response.getPageInfo().getOffset());
        assertEquals(limit, response.getPageInfo().getLimit());
        assertEquals((int)limit, response.getItems().size());
        assertNotNull(response.getItems().get(0).getCdTenantIds());
    }

    @Test
    public void testListTenantsErr() {
        // Setup
        final long offset = 0L;
        final long limit = 1000L;
        when(vaultAdminMock.listAccounts(anyLong(),any(ListAccountsRequestDTO.class)))
                .thenAnswer((Answer<ListAccountsResponseDTO>) invocation -> {
                    throw new VaultServiceException(HttpStatus.BAD_REQUEST, "Requested offset is outside the total available items");
                });

        final PageOfTenants response = scalityOsisServiceUnderTest.listTenants(offset, limit);

        // Verify the results
        assertEquals(0L, response.getPageInfo().getTotal());
        assertEquals(offset, response.getPageInfo().getOffset());
        assertEquals(limit, response.getPageInfo().getLimit());
        assertEquals(0L, response.getItems().size());

    }

    @Test
    public void testQueryTenants() {
        // Setup
        final long offset = 0L;
        final long limit = 1000L;
        final String filter = CD_TENANT_ID_PREFIX + TEST_STR;

        // Run the test
        // Call Scality Osis service to query tenants
        final PageOfTenants response = scalityOsisServiceUnderTest.queryTenants(offset, limit, filter);

        // Verify the results
        assertEquals(1, response.getPageInfo().getTotal());
        assertEquals(offset, response.getPageInfo().getOffset());
        assertEquals(limit, response.getPageInfo().getLimit());
        assertEquals(1, response.getItems().size());
        assertEquals(TEST_STR, response.getItems().get(0).getCdTenantIds().get(0));
    }

    @Test
    public void testQueryTenantsOffset() {
        // Setup
        final long offset = 2000L;
        final long limit = 1000L;
        final String filter = CD_TENANT_ID_PREFIX + TEST_STR;
        // Run the test
        // Call Scality Osis service to list tenants
        final PageOfTenants response = scalityOsisServiceUnderTest.queryTenants(offset, limit, filter);


        // Verify the results
        assertEquals(1, response.getPageInfo().getTotal());
        assertEquals(offset, response.getPageInfo().getOffset());
        assertEquals(limit, response.getPageInfo().getLimit());
        assertEquals(1, response.getItems().size());
        assertEquals(TEST_STR, response.getItems().get(0).getCdTenantIds().get(0));
    }

    @Test
    public void testQueryTenantsNoFilter() {
        // Setup
        final long offset = 0L;
        final long limit = 1000L;
        final String filter = "";

        // Run the test
        // Call Scality Osis service to query tenants
        final PageOfTenants response = scalityOsisServiceUnderTest.queryTenants(offset, limit, filter);

        // Should return the response as list tenants
        assertEquals(limit, response.getPageInfo().getTotal());
        assertEquals(offset, response.getPageInfo().getOffset());
        assertEquals(limit, response.getPageInfo().getLimit());
        assertEquals((int)limit, response.getItems().size());
        assertNotNull(response.getItems().get(0).getCdTenantIds());
    }

    @Test
    public void testQueryTenantsErr() {
        // Setup
        final long offset = 3000L;
        final long limit = 1000L;
        final String filter = CD_TENANT_ID_PREFIX + TEST_STR;

        when(vaultAdminMock.listAccounts(anyLong(),any(ListAccountsRequestDTO.class)))
                .thenAnswer((Answer<ListAccountsResponseDTO>) invocation -> {
                    throw new VaultServiceException(HttpStatus.BAD_REQUEST, "Requested offset is outside the total available items");
                });

        final PageOfTenants response = scalityOsisServiceUnderTest.queryTenants(offset, limit, filter);

        // Verify the results
        assertEquals(0L, response.getPageInfo().getTotal());
        assertEquals(offset, response.getPageInfo().getOffset());
        assertEquals(limit, response.getPageInfo().getLimit());
        assertEquals(0L, response.getItems().size());

    }

    @Test
    public void testCreateUser() {
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
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.createUser(osisUser), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

    @Test
    public void testQueryUsers() {
        // Setup

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.queryUsers(0L, 0L, "filter"), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

    @Test
    public void testCreateS3Credential() {
        // Setup

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.createS3Credential(TEST_TENANT_ID, TEST_USER_ID), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

    @Test
    public void testQueryS3Credentials() {
        // Setup

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.queryS3Credentials(0L, 0L, "filter"), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

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
    public void testDeleteS3Credential() {
        // Setup

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.deleteS3Credential(TEST_TENANT_ID, TEST_USER_ID, "accessKey"), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

    @Test
    public void testDeleteTenant() {
        // Setup

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.deleteTenant(TEST_TENANT_ID, false), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

    @Test
    public void testUpdateTenant() {
        // Setup
        final OsisTenant osisTenant = new OsisTenant();
        osisTenant.active(false);
        osisTenant.name(TEST_NAME);
        osisTenant.setName(TEST_NAME);
        osisTenant.tenantId(TEST_TENANT_ID);
        osisTenant.setTenantId(TEST_TENANT_ID);
        osisTenant.cdTenantIds(Arrays.asList(TEST_STR));
        osisTenant.setCdTenantIds(Arrays.asList(TEST_STR));

        final OsisTenant expectedResult = new OsisTenant();
        expectedResult.active(false);
        expectedResult.name(TEST_NAME);
        expectedResult.setName(TEST_NAME);
        expectedResult.tenantId(TEST_TENANT_ID);
        expectedResult.setTenantId(TEST_TENANT_ID);
        expectedResult.cdTenantIds(Arrays.asList(TEST_STR));
        expectedResult.setCdTenantIds(Arrays.asList(TEST_STR));

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.updateTenant(TEST_TENANT_ID, osisTenant), NOT_IMPLEMENTED_EXCEPTION_ERR);

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
    public void testGetTenant() {
        // Setup
        final OsisTenant expectedResult = new OsisTenant();
        expectedResult.active(false);
        expectedResult.name(TEST_NAME);
        expectedResult.setName(TEST_NAME);
        expectedResult.tenantId(TEST_TENANT_ID);
        expectedResult.setTenantId(TEST_TENANT_ID);
        expectedResult.cdTenantIds(Arrays.asList(TEST_STR));
        expectedResult.setCdTenantIds(Arrays.asList(TEST_STR));

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.getTenant(TEST_TENANT_ID), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

    @Test
    public void testGetUser1() {
        // Setup

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.getUser("canonicalUserId"), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

    @Test
    public void testGetUser2() {
        // Setup

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.getUser(TEST_TENANT_ID, TEST_USER_ID), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

    @Test
    public void testHeadTenant() {
        // Setup

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.headTenant(TEST_TENANT_ID), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
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

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.listS3Credentials(TEST_TENANT_ID, TEST_USER_ID, 0L, 0L), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

    @Test
    public void testListUsers() {
        // Setup

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.listUsers(TEST_TENANT_ID, 0L, 0L), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
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

    @Test
    public void testGetInformationWithBasicAuth() {
        // Setup
        final String domain = "https://localhost:8443";

        // Run the test
        final Information information = scalityOsisServiceUnderTest.getInformation(domain);

        // Verify the results
        assertEquals(Information.AuthModesEnum.BASIC, information.getAuthModes().get(0), "Invalid AuthModes" );
        assertNotNull(information.getStorageClasses(), NULL_ERR );
        assertNotNull(information.getRegions(), NULL_ERR );
        assertEquals(PLATFORM_NAME, information.getPlatformName(), "Invalid Platform name");
        assertEquals(PLATFORM_VERSION, information.getPlatformVersion(), "Invalid Platform Version");
        assertEquals(API_VERSION, information.getApiVersion(), "Invalid API Version");
        assertEquals(Information.StatusEnum.NORMAL, information.getStatus(), "Invalid status");
        assertEquals(TEST_S3_INTERFACE_URL, information.getServices().getS3(), "Invalid S3 interface URL");
        assertNotNull(information.getNotImplemented(), NULL_ERR);
        assertEquals(domain + IAM_PREFIX,  information.getServices().getIam(), "Invalid IAM URL");

    }

    @Test
    public void testGetInformationWithBearerAuth() {
        // Setup
        final String domain = "https://localhost:8443";
        when(appEnvMock.isApiTokenEnabled()).thenReturn(true);

        // Run the test
        final Information information = scalityOsisServiceUnderTest.getInformation(domain);

        // Verify the results
        assertEquals(Information.AuthModesEnum.BEARER, information.getAuthModes().get(0), "Invalid AuthModes" );
        assertNotNull(information.getStorageClasses(), NULL_ERR );
        assertNotNull(information.getRegions(), NULL_ERR );
        assertEquals(PLATFORM_NAME, information.getPlatformName(), "Invalid Platform name");
        assertEquals(PLATFORM_VERSION, information.getPlatformVersion(), "Invalid Platform Version");
        assertEquals(API_VERSION, information.getApiVersion(), "Invalid API Version");
        assertEquals(Information.StatusEnum.NORMAL, information.getStatus(), "Invalid status");
        assertEquals(TEST_S3_INTERFACE_URL, information.getServices().getS3(), "Invalid S3 interface URL");
        assertNotNull(information.getNotImplemented(), NULL_ERR);
        assertEquals(domain + IAM_PREFIX,  information.getServices().getIam(), "Invalid IAM URL");
    }

    @Test
    public void testUpdateOsisCaps() {
        // Setup
        final OsisCaps osisCaps = new OsisCaps();

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.updateOsisCaps(osisCaps), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

    @Test
    public void testGetBucketList() {
        // Setup

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.getBucketList(TEST_TENANT_ID, 0L, 0L), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
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
    public void testGetCredentialsWithNoRoleFail() {
        // Setup

        when(vaultAdminMock.getTempAccountCredentials(any(AssumeRoleRequest.class)))
                .thenThrow(new VaultServiceException(HttpStatus.NOT_FOUND, "NoSuchEntity", "Role does not exist"))
                .thenThrow(new VaultServiceException(HttpStatus.NOT_FOUND, "MalformedPolicyDocument"));
        // Run the test
        // Verify the results
        assertThrows(VaultServiceException.class, () -> scalityOsisServiceUnderTest.getCredentials(TEST_TENANT_ID));
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

        // Verify if all the API calls were skipped after create role if it returns "NoSuchEntity"
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

        // Verify if all the API calls were made successfully even after createPolicy returns "EntityAlreadyExists"
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

        // Verify if all the API calls were skipped after attachRolePolicy if it returns "NoSuchEntity"
        verify(vaultAdminMock).getAccountAccessKey(any(GenerateAccountAccessKeyRequest.class));
        verify(iamMock).createRole(any(CreateRoleRequest.class));
        verify(iamMock).createPolicy(any(CreatePolicyRequest.class));
        verify(iamMock).attachRolePolicy(any(AttachRolePolicyRequest.class));
        verify(iamMock, never()).deleteAccessKey(any(DeleteAccessKeyRequest.class));
    }


}
