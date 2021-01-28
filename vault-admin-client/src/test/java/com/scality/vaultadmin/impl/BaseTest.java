/**
 *Copyright 2020 VMware, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.vaultadmin.impl;

import com.amazonaws.Response;
import com.amazonaws.http.HttpResponse;
import com.scality.vaultclient.dto.AccountData;
import com.scality.vaultclient.dto.CreateAccountRequestDTO;
import com.scality.vaultclient.dto.CreateAccountResponseDTO;
import com.scality.vaultclient.services.AccountServicesClient;
import com.scality.vaultclient.services.VaultClientException;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

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

  private static final String DEFAULT_TEST_CANONICAL_ID = "31e38bcfda3ab1887587669ee25a348cc89e6e2e87dc38088289b1b3c5329b30";

  protected final String ENTITY_EXISTS_ERR = "The request was rejected because it attempted to create a resource that already exists.";

  @BeforeEach
  public void init() throws IOException {
    initProps();
    initMocks();
  }

  protected void initMocks() {
    accountServicesClient = mock(AccountServicesClient.class);

    //initialize mock create account response
    when(accountServicesClient.createAccount(any(CreateAccountRequestDTO.class)))
    .thenAnswer(new Answer<Response<CreateAccountResponseDTO>>() {
      public Response<CreateAccountResponseDTO> answer(InvocationOnMock invocation) {
        CreateAccountRequestDTO request = invocation.getArgument(0);
        AccountData data = new AccountData();
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
        com.scality.vaultclient.dto.Account account = new com.scality.vaultclient.dto.Account();
        account.setData(data);
        CreateAccountResponseDTO response = new CreateAccountResponseDTO();
        response.setAccount(account);

        HttpResponse httpResponse = new HttpResponse(null, null);
        httpResponse.setStatusCode(201);
        httpResponse.setStatusText("Created");
        return new Response<>(response,httpResponse);
      }
    });


    vaultAdminImpl = new VaultAdminImpl(accountServicesClient, adminEndpoint);
  }

  protected void loadExistingAccountErrorMocks() {
    final String ENTITY_EXISTS_ERR = "The request was rejected because it attempted to create a resource that already exists.";

    //initialize mock create account response
    when(accountServicesClient.createAccount(any(CreateAccountRequestDTO.class)))
            .thenAnswer(new Answer<Response<CreateAccountResponseDTO>>() {
              public Response<CreateAccountResponseDTO> answer(InvocationOnMock invocation) {
                  VaultClientException e = new VaultClientException("EntityAlreadyExists");
                  e.setErrorCode("EntityAlreadyExists");
                  e.setErrorMessage(ENTITY_EXISTS_ERR);
                  e.setStatusCode(409);
                  e.setServiceName("Vault");
                  throw e;
                }
            });
  }

  private void initProps() throws IOException {
    String env = System.getProperty("env", "");
    if (!"".equals(env)) {
      env = "." + env;
    }
    Properties properties = new Properties();
    properties.load(VaultAdminImplTest.class.getResourceAsStream("/vaultadmin.properties" + env));

    adminUserId = properties.getProperty("vault.adminId");
    adminAccessKey = properties.getProperty("vault.adminAccessKey");
    adminSecretKey = properties.getProperty("vault.adminSecretKey");
    s3Endpoint = properties.getProperty("vault.endpoint");
    adminEndpoint = properties.getProperty("vault.adminEndpoint");
  }

  protected void testWithAnAccount(Consumer<CreateAccountResponseDTO> test) {
    String email_address = UUID.randomUUID().toString() +"_"+ UUID.randomUUID().toString() + "@osis.account.com";
    CreateAccountRequestDTO createAccountRequestDTO = CreateAccountRequestDTO.builder()
            .emailAddress(email_address)
            .name("tenant.name__bfc0d4a51e06481cbc917e9d96e52d81")
//                    .externalAccountId("249349283982")
//                    .quotaMax(10)
            .build();
    try {
      CreateAccountResponseDTO response = vaultAdminImpl.createAccount(createAccountRequestDTO);
      test.accept(response);
    } finally {
    }
  }
}
