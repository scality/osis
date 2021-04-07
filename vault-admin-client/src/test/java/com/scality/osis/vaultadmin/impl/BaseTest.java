package com.scality.osis.vaultadmin.impl;

import com.amazonaws.Response;
import com.amazonaws.http.HttpResponse;
import com.scality.vaultclient.dto.AccountData;
import com.scality.vaultclient.dto.CreateAccountRequestDTO;
import com.scality.vaultclient.dto.CreateAccountResponseDTO;
import com.scality.vaultclient.dto.ListAccountsRequestDTO;
import com.scality.vaultclient.dto.ListAccountsResponseDTO;
import com.scality.vaultclient.services.AccountServicesClient;
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
  protected static OkHttpClient okHttpClient;
  protected static String adminUserId;
  protected static String adminAccessKey;
  protected static String adminSecretKey;
  protected static String s3Endpoint;
  protected static String adminEndpoint;

  private static final String DEFAULT_TEST_ACCOUNT_ID = "001583654825";

  private static final String DEFAULT_TEST_ARN_STR = "\"arn:aws:iam::001583654825:/";

  private static final String DEFAULT_TEST_ACCOUNT_NAME = "Account5425";

  private static final String DEFAULT_TEST_EMAIL_ADDR = "xyz@scality.com";

  private static final String DEFAULT_TEST_CANONICAL_ID = "31e38bcfda3ab1887587669ee25a348cc89e6e2e87dc38088289b1b3c5329b30";

  protected static final String ENTITY_EXISTS_ERR = "The request was rejected because it attempted to create a resource that already exists.";

  @BeforeEach
  public void init() throws IOException {
    initProps();
    initMocks();
  }

  protected void initMocks() {
    accountServicesClient = mock(AccountServicesClient.class);

    initCreateAccountMocks();
    initListAccountsMocks();

    vaultAdminImpl = new VaultAdminImpl(accountServicesClient, adminEndpoint);
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
                    customAttributestemp.put("cd_tenant_id%3D%3D" + UUID.randomUUID(), "");
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
    s3Endpoint = properties.getProperty("vault.endpoint");
    adminEndpoint = properties.getProperty("vault.adminEndpoint");
  }
}
