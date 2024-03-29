package com.scality.osis.service.impl;

import com.amazonaws.Response;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.User;
import com.amazonaws.services.identitymanagement.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.Owner;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.scality.osis.ScalityAppEnv;
import com.scality.osis.model.OsisUser;
import com.scality.osis.redis.service.ScalityRedisRepository;
import com.scality.osis.resource.ScalityOsisCapsManager;
import com.scality.osis.s3.impl.S3Impl;
import com.scality.osis.security.crypto.BaseCipher;
import com.scality.osis.security.crypto.model.CipherInformation;
import com.scality.osis.security.crypto.model.SecretKeyRepoData;
import com.scality.osis.security.utils.CipherFactory;
import com.scality.osis.utapi.impl.UtapiImpl;
import com.scality.osis.utapiclient.dto.MetricsData;
import com.scality.osis.utapiclient.services.UtapiServiceClient;
import com.scality.osis.vaultadmin.impl.VaultAdminImpl;
import com.scality.osis.vaultadmin.impl.cache.CacheFactory;
import com.scality.osis.vaultadmin.impl.cache.CacheImpl;
import com.scality.vaultclient.dto.*;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.SecureRandom;
import java.util.*;

import static com.scality.osis.security.utils.SecurityConstants.DEFAULT_AES_GCM_TAG_LENGTH;
import static com.scality.osis.security.utils.SecurityConstants.NAME_AES_256_GCM_CIPHER;
import static com.scality.osis.utils.ScalityConstants.CD_TENANT_ID_PREFIX;
import static com.scality.osis.utils.ScalityConstants.REDIS_SPRING_CACHE_TYPE;
import static com.scality.osis.utils.ScalityTestUtils.*;
import static com.scality.osis.vaultadmin.impl.cache.CacheConstants.DEFAULT_CACHE_MAX_CAPACITY;
import static com.scality.osis.vaultadmin.impl.cache.CacheConstants.NAME_LIST_ACCOUNTS_CACHE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"PMD.ExcessiveImports", "PMD.TooManyStaticImports"})
class BaseOsisServiceTest {

    private static final SecureRandom RANDOM = new SecureRandom();

    // vault admin mock object
    @Mock
    protected VaultAdminImpl vaultAdminMock;

    @Mock
    protected S3Impl s3Mock;

    @Mock
    protected UtapiImpl utapiMock;

    protected ScalityOsisServiceImpl scalityOsisServiceUnderTest;

    protected AsyncScalityOsisService asyncScalityOsisServiceUnderTest;

    @Mock
    protected ScalityAppEnv appEnvMock;

    @Mock
    protected ScalityOsisCapsManager osisCapsManagerMock;

    @Mock
    protected AmazonIdentityManagement iamMock;

    @Mock
    protected AmazonS3 s3ClientMock;

    @Mock
    protected UtapiServiceClient utapiServiceClientMock;

    @Mock
    protected ScalityRedisRepository<SecretKeyRepoData> redisRepositoryMock;

    @Mock
    protected CipherFactory cipherFactoryMock;

    @Mock
    protected BaseCipher baseCipherMock;

    @BeforeEach
    protected void init() {
        MockitoAnnotations.openMocks(this);
        initMocks();
        scalityOsisServiceUnderTest = new ScalityOsisServiceImpl(appEnvMock, vaultAdminMock, s3Mock, utapiMock, osisCapsManagerMock);

        asyncScalityOsisServiceUnderTest = new AsyncScalityOsisService();
        ReflectionTestUtils.setField(asyncScalityOsisServiceUnderTest, "vaultAdmin", vaultAdminMock);
        ReflectionTestUtils.setField(asyncScalityOsisServiceUnderTest, "appEnv", appEnvMock);

        ReflectionTestUtils.setField(scalityOsisServiceUnderTest, "asyncScalityOsisService",
                asyncScalityOsisServiceUnderTest);
        ReflectionTestUtils.setField(scalityOsisServiceUnderTest, "scalityRedisRepository", redisRepositoryMock);
        ReflectionTestUtils.setField(scalityOsisServiceUnderTest, "cipherFactory", cipherFactoryMock);
    }

    protected void initMocks() {
        when(appEnvMock.getConsoleEndpoint()).thenReturn(TEST_CONSOLE_URL);
        when(appEnvMock.isApiTokenEnabled()).thenReturn(false);
        when(appEnvMock.getStorageInfo()).thenReturn(Collections.singletonList("standard"));
        when(appEnvMock.getRegionInfo()).thenReturn(Collections.singletonList("default"));
        when(appEnvMock.getPlatformName()).thenReturn(PLATFORM_NAME);
        when(appEnvMock.getPlatformVersion()).thenReturn(PLATFORM_VERSION);
        when(appEnvMock.getApiVersion()).thenReturn(API_VERSION);
        when(appEnvMock.getS3InterfaceEndpoint()).thenReturn(TEST_S3_INTERFACE_URL);
        when(appEnvMock.getS3Endpoint()).thenReturn(TEST_S3_URL);
        when(appEnvMock.isUtapiEnabled()).thenReturn(true);
        when(appEnvMock.getUtapiEndpoint()).thenReturn(TEST_UTAPI_URL);
        when(appEnvMock.getAssumeRoleName()).thenReturn(SAMPLE_ASSUME_ROLE_NAME);
        when(appEnvMock.getSpringCacheType()).thenReturn(REDIS_SPRING_CACHE_TYPE);
        when(appEnvMock.getS3CapabilitiesFilePath()).thenReturn(TEST_S3_CAPABILITIES_FILE_PATH);
        when(osisCapsManagerMock.getNotImplements()).thenReturn(new ArrayList<>());
        when(vaultAdminMock.getIAMClient(any(Credentials.class), any())).thenReturn(iamMock);
        when(s3Mock.getS3Client(any(Credentials.class), any())).thenReturn(s3ClientMock);
        when(utapiMock.getUtapiServiceClient(any(Credentials.class), any())).thenReturn(utapiServiceClientMock);

        initCreateTenantMocks();
        initUpdateTenantMocks();
        initListTenantMocks();
        initTempCredentialsMocks();
        initCreatePolicyMocks();
        initGenerateAccountAKMocks();
        initCreateRoleMocks();
        initAttachRolePolicyMocks();
        initDeleteAccessKeyMocks();
        initGetAccountMocks();
        initGetPolicyMocks();
        initDetachUserPolicyMocks();
        initCreateAccessRequestMocks();
        initCreateUserMocks();
        initListUserMocks();
        initGetUserMocks();
        initDeleteUserMocks();
        initListAccessKeysMocks();
        initUpdateAccessKeysMocks();
        initGetBucketListMocks();
        initCaches();
        initBaseCipherMocks();
        initCipherFactoryMocks();
        initRedisMocks();
    }

    private void initBaseCipherMocks() {
        try {
            when(baseCipherMock.encrypt(any(), any(), any())).thenReturn(mockSecretKeyRepoData());
            when(baseCipherMock.decrypt(any(), any(), any())).thenReturn(TEST_SECRET_KEY);
        } catch (Exception e) {
            init();
        }
    }

    private void initCipherFactoryMocks() {
        when(cipherFactoryMock.getCipher()).thenReturn(baseCipherMock);
        when(cipherFactoryMock.getCipherByID(any())).thenReturn(baseCipherMock);
        when(cipherFactoryMock.getCipherByName(any())).thenReturn(baseCipherMock);
        when(cipherFactoryMock.getLatestCipherID()).thenReturn("1");
        when(cipherFactoryMock.getSecretCipherKeyByID(any())).thenReturn(TEST_CIPHER_SECRET_KEY);
        when(cipherFactoryMock.getLatestSecretCipherKey()).thenReturn(TEST_CIPHER_SECRET_KEY);
        when(cipherFactoryMock.getLatestCipherName()).thenReturn(NAME_AES_256_GCM_CIPHER);
    }

    private void initRedisMocks() {
        when(redisRepositoryMock.get(any())).thenReturn(mockSecretKeyRepoData());
        when(redisRepositoryMock.hasKey(any())).thenReturn(Boolean.TRUE);
    }

    private SecretKeyRepoData mockSecretKeyRepoData() {
        final SecretKeyRepoData secretKeyRepoData = new SecretKeyRepoData();

        secretKeyRepoData.setKeyID("1");

        final byte[] encryptedBytes = new byte[DEFAULT_AES_GCM_TAG_LENGTH];
        RANDOM.nextBytes(encryptedBytes);
        secretKeyRepoData.setEncryptedBytes(encryptedBytes);

        final CipherInformation cipherInfo = new CipherInformation();
        cipherInfo.setCipherName(NAME_AES_256_GCM_CIPHER);
        secretKeyRepoData.setCipherInfo(cipherInfo);

        return secretKeyRepoData;
    }

    protected void initCreateTenantMocks() {
        // initialize mock create account response
        when(vaultAdminMock.createAccount(any(CreateAccountRequestDTO.class)))
                .thenAnswer((Answer<CreateAccountResponseDTO>) invocation -> {
                    final CreateAccountRequestDTO request = invocation.getArgument(0);
                    final AccountData data = new AccountData();
                    data.setEmailAddress(request.getEmailAddress());
                    data.setName(request.getName());
                    if (StringUtils.isEmpty(request.getExternalAccountId())) {
                        data.setId(SAMPLE_TENANT_ID);
                    } else {
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

    protected void initUpdateTenantMocks() {
        // initialize mock create account response
        when(vaultAdminMock.updateAccountAttributes(any(UpdateAccountAttributesRequestDTO.class)))
                .thenAnswer((Answer<CreateAccountResponseDTO>) invocation -> {
                    final UpdateAccountAttributesRequestDTO request = invocation.getArgument(0);
                    final AccountData data = new AccountData();
                    data.setEmailAddress(SAMPLE_SCALITY_ACCOUNT_EMAIL);
                    data.setName(request.getName());
                    data.setId(SAMPLE_ID);
                    data.setCustomAttributes(request.getCustomAttributes());
                    final Account account = new Account();
                    account.setData(data);
                    final CreateAccountResponseDTO response = new CreateAccountResponseDTO();
                    response.setAccount(account);

                    return response;
                });
    }

    protected void initListTenantMocks() {

        // initialize mock list accounts response
        when(vaultAdminMock.listAccounts(anyLong(), any(ListAccountsRequestDTO.class)))
                .thenAnswer((Answer<ListAccountsResponseDTO>) invocation -> {
                    final long offset = invocation.getArgument(0);
                    final ListAccountsRequestDTO request = invocation.getArgument(1);
                    final int maxItems = request.getMaxItems();
                    final List<AccountData> accounts = new ArrayList<>();

                    // Generate Accounts with ids (markerVal + i) to maxItems count
                    for (int index = 0; index < maxItems; index++) {
                        final AccountData data = new AccountData();
                        data.setEmailAddress("xyz@scality.com");
                        data.setName(TEST_NAME);
                        data.setId(SAMPLE_TENANT_ID + (index + offset)); // setting ID with index

                        // if filterStartsWith generate customAttributes for all accounts
                        final Map<String, String> customAttributestemp = new HashMap<>();
                        data.setCustomAttributes(customAttributestemp);
                        accounts.add(data);

                        if (!StringUtils.isEmpty(request.getFilterKey())
                                && request.getFilterKey().startsWith(CD_TENANT_ID_PREFIX)) {
                            // If filter key exists mock only one account in the response with filterKey as
                            // customAttributestemp
                            customAttributestemp.put(request.getFilterKey(), "");
                            break;
                        } else {
                            customAttributestemp.put(CD_TENANT_ID_PREFIX + UUID.randomUUID(), "");
                        }
                    }

                    final ListAccountsResponseDTO response = new ListAccountsResponseDTO();
                    response.setAccounts(accounts);
                    response.setMarker("M" + (offset + maxItems));
                    response.setTruncated(true);
                    return response;
                });
    }

    protected void initTempCredentialsMocks() {
        // initialize mock vault admin temporary credentials response
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

    protected void initGenerateAccountAKMocks() {
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

    protected void initCreateRoleMocks() {
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
                            .withArn("arn:aws:iam::" + TEST_TENANT_ID + ":role/" + request.getRoleName());
                    response.setRole(role);
                    return response;
                });
    }

    protected void initAttachRolePolicyMocks() {
        when(iamMock.attachRolePolicy(any(AttachRolePolicyRequest.class))).thenReturn(new AttachRolePolicyResult());
    }

    protected void initDeleteAccessKeyMocks() {
        when(iamMock.deleteAccessKey(any(DeleteAccessKeyRequest.class))).thenReturn(new DeleteAccessKeyResult());
    }

    protected void initCreatePolicyMocks() {
        when(iamMock.createPolicy(any(CreatePolicyRequest.class)))
                .thenAnswer((Answer<CreatePolicyResult>) invocation -> {
                    final CreatePolicyRequest request = invocation.getArgument(0);
                    final CreatePolicyResult response = new CreatePolicyResult();
                    final Policy policy = new Policy()
                            .withPolicyName(request.getPolicyName())
                            .withDescription(request.getDescription())
                            .withArn("arn:aws:iam::" + TEST_TENANT_ID + ":policy/" + request.getPolicyName());
                    response.setPolicy(policy);
                    return response;
                });
    }

    protected void initGetAccountMocks() {
        when(vaultAdminMock.getAccount(any(GetAccountRequestDTO.class)))
                .thenAnswer((Answer<AccountData>) invocation -> {

                    final GetAccountRequestDTO request = invocation.getArgument(0);
                    final String accountId = request.getAccountId() == null ? SAMPLE_TENANT_ID : request.getAccountId();
                    final String accountName = request.getAccountName() == null ? SAMPLE_TENANT_NAME
                            : request.getAccountName();
                    final String accountArn = request.getAccountArn();
                    final String emailAddress = request.getEmailAddress() == null ? SAMPLE_SCALITY_ACCOUNT_EMAIL
                            : request.getEmailAddress();
                    final String canonicalId = request.getCanonicalId() == null ? TEST_CANONICAL_ID
                        : request.getCanonicalId();
                    final Map<String, String> customAttributestes = new HashMap<>();
                    customAttributestes.put(CD_TENANT_ID_PREFIX + SAMPLE_CD_TENANT_ID, "");

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

    protected void initCreateUserMocks() {

        when(iamMock.createUser(any(CreateUserRequest.class)))
                .thenAnswer((Answer<CreateUserResult>) invocation -> createUserMockResponse(invocation));
    }

    protected CreateUserResult createUserMockResponse(final InvocationOnMock invocation) {
        final CreateUserRequest request = invocation.getArgument(0);
        final User user = new User()
                .withUserId(TEST_USER_ID)
                .withUserName(request.getUserName())
                .withPath(request.getPath())
                .withCreateDate(new Date());

        return new CreateUserResult().withUser(user);
    }

    protected void initGetPolicyMocks() {
        when(iamMock.getPolicy(any(GetPolicyRequest.class)))
                .thenAnswer((Answer<GetPolicyResult>) invocation -> {
                    final GetPolicyRequest request = invocation.getArgument(0);
                    final GetPolicyResult response = new GetPolicyResult();
                    final Policy policy = new Policy()
                            .withArn(request.getPolicyArn());
                    response.setPolicy(policy);
                    return response;
                });
    }

    protected void initDetachUserPolicyMocks() {
        when(iamMock.detachUserPolicy(any(DetachUserPolicyRequest.class)))
                .thenAnswer((Answer<DetachUserPolicyResult>) invocation -> new DetachUserPolicyResult());
    }

    protected void initCreateAccessRequestMocks() {
        when(iamMock.createAccessKey(any(CreateAccessKeyRequest.class)))
                .thenAnswer((Answer<CreateAccessKeyResult>) invocation -> createAccessKeyMockResponse(invocation));
    }

    protected CreateAccessKeyResult createAccessKeyMockResponse(final InvocationOnMock invocation) {
        final CreateAccessKeyRequest request = invocation.getArgument(0);
        final AccessKey accessKeyObj = new AccessKey()
                .withAccessKeyId(TEST_ACCESS_KEY)
                .withSecretAccessKey(TEST_SECRET_KEY)
                .withCreateDate(new Date())
                .withUserName(request.getUserName())
                .withStatus(StatusType.Active);
        return new CreateAccessKeyResult()
                .withAccessKey(accessKeyObj);
    }

    protected void initListAccessKeysMocks() {
        when(iamMock.listAccessKeys(any(ListAccessKeysRequest.class)))
                .thenAnswer((Answer<ListAccessKeysResult>) invocation -> listAccessKeysMockResponse(invocation));
    }

    protected ListAccessKeysResult listAccessKeysMockResponse(final InvocationOnMock invocation) {
        final ListAccessKeysRequest request = invocation.getArgument(0);

        final AccessKeyMetadata accessKeyMetadata = new AccessKeyMetadata()
                .withAccessKeyId(TEST_ACCESS_KEY)
                .withCreateDate(new Date())
                .withStatus(StatusType.Active)
                .withUserName(request.getUserName());

        return new ListAccessKeysResult()
                .withAccessKeyMetadata(Collections.singletonList(accessKeyMetadata));
    }

    protected ListAccessKeysResult listInactiveAccessKeysMockResponse(final InvocationOnMock invocation) {
        final ListAccessKeysRequest request = invocation.getArgument(0);

        final AccessKeyMetadata accessKeyMetadata = new AccessKeyMetadata()
                .withAccessKeyId(TEST_ACCESS_KEY)
                .withCreateDate(new Date())
                .withStatus(StatusType.Inactive)
                .withUserName(request.getUserName());

        return new ListAccessKeysResult()
                .withAccessKeyMetadata(Collections.singletonList(accessKeyMetadata));
    }

    protected void initListUserMocks() {

        // initialize mock list accounts response
        when(iamMock.listUsers(any(ListUsersRequest.class)))
                .thenAnswer((Answer<ListUsersResult>) invocation -> listUsersMockResponse(invocation));
    }

    protected ListUsersResult listUsersMockResponse(final InvocationOnMock invocation) {
        final ListUsersRequest request = invocation.getArgument(0);
        int maxItems = request.getMaxItems();
        final int markerVal = (request.getMarker() == null) ? 0 : Integer.parseInt(request.getMarker());
        final String pathPrefix = request.getPathPrefix();

        if (!StringUtils.isEmpty(pathPrefix)) {
            maxItems = 1;
        }

        final List<User> users = new ArrayList<>();

        // Generate Users with ids (markerVal + i) to maxItems count
        for (int index = 0; index < maxItems; index++) {
            final String pathFirstSection = StringUtils.isEmpty(pathPrefix)
                    ? "/" + TEST_NAME + (index + markerVal) + "/"
                    : pathPrefix;

            final String path = pathFirstSection
                    + OsisUser.RoleEnum.TENANT_USER.getValue() + "/"
                    + SAMPLE_SCALITY_USER_EMAIL + "/"
                    + TEST_TENANT_ID + "/";

            final User user = new User()
                    .withUserId(TEST_USER_ID + (index + markerVal))
                    .withUserName(TEST_NAME + (index + markerVal))
                    .withPath(path)
                    .withCreateDate(new Date());

            users.add(user);
        }

        return new ListUsersResult()
                .withUsers(users)
                .withIsTruncated(Boolean.TRUE)
                .withMarker(markerVal + maxItems + "");
    }

    protected void initGetUserMocks() {

        // initialize mock get user response
        when(iamMock.getUser(any(GetUserRequest.class)))
                .thenAnswer((Answer<GetUserResult>) invocation -> getUserMockResponse(invocation));
    }

    protected void initDeleteUserMocks() {

        // initialize mock get user response
        when(iamMock.deleteUser(any(DeleteUserRequest.class)))
                .thenAnswer((Answer<DeleteUserResult>) invocation -> new DeleteUserResult());
    }

    protected void initUpdateAccessKeysMocks() {

        // initialize mock update access keys response
        when(iamMock.updateAccessKey(any(UpdateAccessKeyRequest.class)))
                .thenAnswer((Answer<UpdateAccessKeyResult>) invocation -> new UpdateAccessKeyResult());
    }

    protected GetUserResult getUserMockResponse(final InvocationOnMock invocation) {
        final GetUserRequest request = invocation.getArgument(0);
        final String username = request.getUserName();

        final String path = "/" + TEST_NAME + "/"
                + OsisUser.RoleEnum.TENANT_USER.getValue() + "/"
                + SAMPLE_SCALITY_USER_EMAIL + "/"
                + TEST_TENANT_ID + "/";

        final User user = new User()
                .withUserId(SAMPLE_ID)
                .withUserName(username)
                .withPath(path)
                .withCreateDate(new Date());

        return new GetUserResult()
                .withUser(user);
    }

    protected void initCaches() {
        final CacheFactory cacheFactoryMock = mock(CacheFactory.class);
        when(cacheFactoryMock.getCache(NAME_LIST_ACCOUNTS_CACHE))
                .thenReturn(new CacheImpl<>(DEFAULT_CACHE_MAX_CAPACITY));

        ReflectionTestUtils.setField(vaultAdminMock, "cacheFactory", cacheFactoryMock);
        vaultAdminMock.initCaches();
    }

    protected void initGetBucketListMocks() {
        // initialize mock s3 list buckets response
        when(s3ClientMock.listBuckets())
                .thenAnswer((Answer<List<Bucket>>) invocation -> getBucketListMockResponse());
    }

    protected List<Bucket> getBucketListMockResponse() {
        final List<Bucket> buckets = new ArrayList<>();

        // create TEST_BUCKET_TOTAL_NUMBER buckets for getBucketList tests
        for (int index = 0; index < TEST_BUCKET_TOTAL_NUMBER; index++) {
            final Bucket bucket = new Bucket();
            bucket.setName(TEST_BUCKET_NAME + (index));
            bucket.setCreationDate(new Date());
            bucket.setOwner(new Owner(TEST_CANONICAL_ID, SAMPLE_TENANT_NAME));

            buckets.add(bucket);
        }

        return buckets;
    }

    protected Response<MetricsData[]> getListMetricsMockResponse() {
        final MetricsData[] metricsData = new MetricsData[1];

        metricsData[0] = new MetricsData();
        metricsData[0].setAccountId(TEST_TENANT_ID);
        metricsData[0].setNumberOfObjects(Arrays.asList(0L, 10L));
        metricsData[0].setStorageUtilized(Arrays.asList(0L, 100L));
        metricsData[0].setIncomingBytes(0L);
        metricsData[0].setOutgoingBytes(0L);
        metricsData[0].setOperations(Map.of("s3:ListObject", 1));
        return new Response<>(metricsData, null);
    }
}
