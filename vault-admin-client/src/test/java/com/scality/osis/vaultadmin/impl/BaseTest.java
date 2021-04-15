package com.scality.osis.vaultadmin.impl;

import com.amazonaws.Response;
import com.amazonaws.http.HttpResponse;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.scality.vaultclient.dto.AccountData;
import com.scality.vaultclient.dto.AssumeRoleResult;
import com.scality.vaultclient.dto.CreateAccountRequestDTO;
import com.scality.vaultclient.dto.CreateAccountResponseDTO;
import com.scality.vaultclient.dto.ListAccountsRequestDTO;
import com.scality.vaultclient.dto.ListAccountsResponseDTO;
import com.scality.vaultclient.services.AccountServicesClient;
import com.scality.vaultclient.services.SecurityTokenServicesClient;
import com.scality.vaultclient.services.VaultClientException;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.platform.commons.util.StringUtils;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.*;

/** Created by saitharun on 2020/12/21. */
public class BaseTest {
  protected VaultAdminImpl vaultAdminImpl;
  protected static AccountServicesClient accountServicesClient;
  protected static SecurityTokenServicesClient stsClient;
  protected static OkHttpClient okHttpClient;
  protected static String adminUserId;
  protected static String adminAccessKey;
  protected static String adminSecretKey;
  protected static String s3InterfaceEndpoint;
  protected static String vaultAdminEndpoint;

  private static final String DEFAULT_TEST_ACCOUNT_ID = "001583654825";

  private static final String DEFAULT_TEST_ARN_STR = "\"arn:aws:iam::001583654825:/";

  private static final String DEFAULT_TEST_ACCOUNT_NAME = "Account5425";

  private static final String DEFAULT_TEST_EMAIL_ADDR = "xyz@scality.com";

  private static final String DEFAULT_TEST_CANONICAL_ID = "31e38bcfda3ab1887587669ee25a348cc89e6e2e87dc38088289b1b3c5329b30";

  protected static final String ENTITY_EXISTS_ERR = "The request was rejected because it attempted to create a resource that already exists.";

  public static final String TEST_ACCESS_KEY = "access_key";
  public static final String TEST_SECRET_KEY = "secret_key";
  public static final String TEST_SESSION_TOKEN = "session_token";
  public static final String TEST_SESSION_NAME = "session";
  public static final String TEST_ROLE_ARN = "arn:aws:iam::123123123123:role/osis";
  public static final String TEST_ASSUMED_USER_ARN = "arn:aws:sts::123123123123:assumed-role/osis/session1";

  @BeforeEach
  public void init() throws IOException {
    initProps();
    initMocks();
  }

  protected void initMocks() {
    accountServicesClient = mock(AccountServicesClient.class);
    stsClient = mock(SecurityTokenServicesClient.class);

    initCreateAccountMocks();
    initListAccountsMocks();
    initAssumeRoleMocks();

    vaultAdminImpl = new VaultAdminImpl(accountServicesClient, stsClient, vaultAdminEndpoint, s3InterfaceEndpoint);
  }

  private void initCreateAccountMocks() {
    //initialize mock create account response
    when(accountServicesClient.createAccount(any(CreateAccountRequestDTO.class)))
            .thenAnswer(new Answer<Response<CreateAccountResponseDTO>>() {
              @Override
              public Response<CreateAccountResponseDTO> answer(final InvocationOnMock invocation) {
                final CreateAccountRequestDTO request = invocation.getArgument(0);
                final AccountData data = new AccountData();
                data.setEmailAddress(request.getEmailAddress());
                data.setName(request.getName());
                data.setArn(DEFAULT_TEST_ARN_STR + request.getName() +"/\"");
                data.setCreateDate(new Date());
                if(request.getExternalAccountId() == null) {
                  data.setId(DEFAULT_TEST_ACCOUNT_ID);
                } else {
                  data.setId(request.getExternalAccountId());
                }
                data.setCanonicalId(DEFAULT_TEST_CANONICAL_ID);
                data.setQuotaMax(request.getQuotaMax());
                final com.scality.vaultclient.dto.Account account = new com.scality.vaultclient.dto.Account();
                account.setData(data);
                final CreateAccountResponseDTO response = new CreateAccountResponseDTO();
                response.setAccount(account);

                final HttpResponse httpResponse = new HttpResponse(null, null);
                httpResponse.setStatusCode(201);
                httpResponse.setStatusText("Created");
                return new Response<>(response,httpResponse);
              }
            });
  }

  private void initListAccountsMocks() {

    //initialize mock list accounts response
    when(accountServicesClient.listAccounts(any(ListAccountsRequestDTO.class)))
            .thenAnswer(new Answer<Response<ListAccountsResponseDTO>>() {
              @Override
              public Response<ListAccountsResponseDTO> answer(final InvocationOnMock invocation) {
                final ListAccountsRequestDTO request = invocation.getArgument(0);
                final String marker = request.getMarker();
                int maxItems = request.getMaxItems();
                final String filterKey = request.getFilterKey();
                final String filterKeyStartsWith = request.getFilterKeyStartsWith();

                Map<String, String> customAttributes1  = null ;
                boolean withcdTenantId = false;

                if(StringUtils.isNotBlank(filterKey)){
                  maxItems = 1;
                  customAttributes1 = new HashMap<>();
                  customAttributes1.put(filterKey,"");
                }

                if(StringUtils.isNotBlank(filterKeyStartsWith)){
                  withcdTenantId = true;
                }

                if(maxItems <= 0){
                  maxItems = 5; // Test Accounts count DEFAULT
                }

                int index = 0;
                int markerVal = 0;
                if(StringUtils.isNotBlank(marker)){
                  markerVal = Integer.parseInt(marker.substring(marker.indexOf("M")+1));
                  // extracting markerVal index at last character
                }

                final List<AccountData> accounts = new ArrayList<>();
                // Generate Accounts with ids (markerVal + i) to maxItems count
                for(; index < maxItems; index++){
                  final AccountData data = new AccountData();
                  data.setEmailAddress(DEFAULT_TEST_EMAIL_ADDR);
                  data.setName(DEFAULT_TEST_ACCOUNT_NAME);
                  data.setArn(DEFAULT_TEST_ARN_STR + DEFAULT_TEST_ACCOUNT_NAME +"/\"");
                  data.setCreateDate(new Date());
                  data.setId(DEFAULT_TEST_ACCOUNT_ID + (index + markerVal)); //setting ID with index
                  data.setCanonicalId(DEFAULT_TEST_CANONICAL_ID);

                  if(withcdTenantId){
                    // if filterStartsWith generate customAttributes for all accounts
                    final Map<String, String> customAttributestemp  = new HashMap<>() ;
                    customAttributestemp.put("cd_tenant_id==" + UUID.randomUUID(), "");
                    data.setCustomAttributes(customAttributestemp);
                  } else {
                    data.setCustomAttributes(customAttributes1);
                  }

                  accounts.add(data);
                }

                final ListAccountsResponseDTO response = new ListAccountsResponseDTO();
                response.setMarker("M"+(markerVal+maxItems));
                response.setTruncated(true);
                response.setAccounts(accounts);

                final HttpResponse httpResponse = new HttpResponse(null, null);
                httpResponse.setStatusCode(200);
                httpResponse.setStatusText("OK");
                return new Response<>(response,httpResponse);
              }
            });
  }

  private void initAssumeRoleMocks() {
    //initialize mock assumerolebackbeat response
    when(stsClient.assumeRoleBackbeat(any(AssumeRoleRequest.class)))
            .thenAnswer(new Answer<Response<AssumeRoleResult>>() {
              @Override
              public Response<AssumeRoleResult> answer(final InvocationOnMock invocation) {
                final Credentials credentials = new Credentials();
                credentials.setAccessKeyId(TEST_ACCESS_KEY);
                credentials.setSecretAccessKey(TEST_SECRET_KEY);
                credentials.setExpiration(new Date());
                credentials.setSessionToken(TEST_SESSION_TOKEN);

                final AssumeRoleResult response = new AssumeRoleResult();
                response.setCredentials(credentials);
                response.setAssumedRoleUser(TEST_ASSUMED_USER_ARN);

                final HttpResponse httpResponse = new HttpResponse(null, null);
                httpResponse.setStatusCode(200);
                httpResponse.setStatusText("OK");
                return new Response<>(response,httpResponse);
              }
    });
  }

  protected void loadExistingAccountErrorMocks() {
    //initialize mock create account response
    when(accountServicesClient.createAccount(any(CreateAccountRequestDTO.class)))
            .thenAnswer(new Answer<Response<CreateAccountResponseDTO>>() {
              @Override
              public Response<CreateAccountResponseDTO> answer(final InvocationOnMock invocation) {
                  final VaultClientException exception = new VaultClientException("EntityAlreadyExists");
                  exception.setErrorCode("EntityAlreadyExists");
                  exception.setErrorMessage(ENTITY_EXISTS_ERR);
                  exception.setStatusCode(409);
                  exception.setServiceName("Vault");
                  throw exception;
                }
            });
  }

  private void initProps() throws IOException {
    String env = System.getProperty("env", "");
    if (!StringUtils.isBlank(env)) {
      env = "." + env;
    }
    final Properties properties = new Properties();
    properties.load(VaultAdminImplTest.class.getResourceAsStream("/vaultadmin.properties" + env));

    adminUserId = properties.getProperty("vault.adminId");
    adminAccessKey = properties.getProperty("vault.adminAccessKey");
    adminSecretKey = properties.getProperty("vault.adminSecretKey");
    s3InterfaceEndpoint = properties.getProperty("vault.endpoint");
    vaultAdminEndpoint = properties.getProperty("vault.adminEndpoint");
  }
}
