/**
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.vaultadmin.impl;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.identitymanagement.model.ListUsersRequest;
import com.amazonaws.services.identitymanagement.model.ListUsersResult;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.util.StringUtils;
import com.google.gson.Gson;
import com.scality.osis.vaultadmin.impl.cache.Cache;
import com.scality.osis.vaultadmin.impl.cache.CacheConstants;
import com.scality.osis.vaultadmin.impl.cache.CacheFactory;
import com.scality.vaultclient.dto.AccountData;
import com.scality.vaultclient.dto.AssumeRoleResult;
import com.scality.vaultclient.dto.CreateAccountRequestDTO;
import com.scality.vaultclient.dto.CreateAccountResponseDTO;
import com.scality.vaultclient.dto.GenerateAccountAccessKeyRequest;
import com.scality.vaultclient.dto.GenerateAccountAccessKeyResponse;
import com.scality.vaultclient.dto.GetAccountRequestDTO;
import com.scality.vaultclient.dto.ListAccountsRequestDTO;
import com.scality.vaultclient.dto.ListAccountsResponseDTO;
import com.scality.vaultclient.services.AccountServicesClient;
import com.scality.vaultclient.services.SecurityTokenServicesClient;
import com.scality.osis.vaultadmin.VaultAdmin;
import okhttp3.HttpUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.amazonaws.services.securitytoken.model.Credentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Vault administrator implementation
 *
 * <p>Created by saitharun on 12/16/20.
 */
@SuppressWarnings({"deprecation", "unchecked"})
@Component
public class VaultAdminImpl implements VaultAdmin{
  private static final Logger logger = LoggerFactory.getLogger(VaultAdminImpl.class);

  public static final String CD_TENANT_ID_PREFIX = "cd_tenant_id";

  private final AccountServicesClient vaultAccountClient;

  private final SecurityTokenServicesClient vaultSTSClient;


  private String vaultAdminEndpoint;

  @Autowired
  private CacheFactory cacheFactory;

  private Cache<Integer, String> listAccountsMarkerCache;

  private Cache<String, Credentials> assumeRoleCache;

  private Cache<String, String> listUsersMarkerCache;

  /**
   * Create a Vault administrator implementation
   *  @param accessKey Access key of the admin who have proper administrative capabilities.
   * @param secretKey Secret key of the admin who have proper administrative capabilities.
   * @param vaultAdminEndpoint Vault admin API endpoint, e.g., http://127.0.0.1:8600
   * @param s3InterfaceEndpoint Vault S3 Interface endpoint, e.g., http://127.0.0.1:8500
   */
  @Autowired
  public VaultAdminImpl(@Value("${osis.scality.vault.access-key}") String accessKey, @Value("${osis.scality.vault.secret-key}") String secretKey, @Value("${osis.scality.vault.endpoint}") String vaultAdminEndpoint, @Value("${osis.scality.vaultS3Interface.endpoint}") String s3InterfaceEndpoint) {
    validEndpoint(vaultAdminEndpoint);
    this.vaultAdminEndpoint = vaultAdminEndpoint;
    this.vaultAccountClient = new AccountServicesClient(
            new BasicAWSCredentials(accessKey, secretKey));
    vaultAccountClient.setEndpoint(vaultAdminEndpoint);


    this.vaultSTSClient = new SecurityTokenServicesClient(
            new BasicAWSCredentials(accessKey, secretKey));
    vaultSTSClient.setEndpoint(s3InterfaceEndpoint);

  }

  /**
   * Create a Vault administrator implementation
   * @param vaultAccountClient Vault Account Service client.
   * @param adminEndpoint Vault admin API endpoint, e.g., https://127.0.0.1:8600
   * @param s3InterfaceEndpoint Vault S3 Interface endpoint, e.g., https://127.0.0.1:8500
   */
  public VaultAdminImpl(AccountServicesClient vaultAccountClient, SecurityTokenServicesClient vaultSTSClient,
                        String adminEndpoint, String s3InterfaceEndpoint) {
    validEndpoint(adminEndpoint);
    vaultAdminEndpoint = adminEndpoint;
    this.vaultAccountClient = vaultAccountClient;
    vaultAccountClient.setEndpoint(adminEndpoint);

    validEndpoint(s3InterfaceEndpoint);
    this.vaultSTSClient = vaultSTSClient;
    vaultSTSClient.setEndpoint(s3InterfaceEndpoint);
  }

  @PostConstruct
  public void initCaches() {
    if(cacheFactory !=null) {
      listAccountsMarkerCache = cacheFactory.getCache(CacheConstants.NAME_LIST_ACCOUNTS_CACHE);
      assumeRoleCache = cacheFactory.getCache(CacheConstants.NAME_ASSUME_ROLE_CACHE);
      listUsersMarkerCache = cacheFactory.getCache(CacheConstants.NAME_LIST_USERS_CACHE);
    }
  }

  /**
   * Validate endpoint. If error throw exception
   * @param endpoint Vault admin API endpoint, e.g., http://127.0.0.1:80/admin
   */
  private static void validEndpoint(String endpoint) {
    if (HttpUrl.parse(endpoint) == null) {
      throw new IllegalArgumentException("endpoint is invalid");
    }
  }

  /**
   * Create a new Account
   *
   * <p>This method will create the account on Vault.
   *
   * @param createAccountRequest CreateAccountRequestDTO object with account name, account externalAccountId.
   * @return The created account response object.
   * @throws VaultServiceException
   */
  @Override
  public CreateAccountResponseDTO createAccount(CreateAccountRequestDTO createAccountRequest) throws VaultServiceException{
    return ExternalServiceFactory.executeVaultService(vaultAccountClient::createAccount, createAccountRequest);
  }

  /**
   * Returns the vault client to invoke account services
   *
   * @return The vault client object.
   */
  @Override
  public AccountServicesClient getVaultAccountclient() {
    return vaultAccountClient;
  }

  /**
   * Returns the vault client to invoke STS services
   *
   * @return The vault client object.
   */
  @Override
  public SecurityTokenServicesClient getVaultSTSclient() {
    return vaultSTSClient;
  }

  /**
   * List accounts
   * <p>This method will list the accounts from Vault.
   *
   * @param listAccountsRequest the list accounts request dto
   * @return the list accounts response dto
   */
  @Override
  public ListAccountsResponseDTO listAccounts(ListAccountsRequestDTO listAccountsRequest) {
    return ExternalServiceFactory.executeVaultService(vaultAccountClient::listAccounts, listAccountsRequest);
  }

  /**
   * List accounts with offset value
   * <p>This method will list the accounts from Vault by computing the marker using the provided offset.
   *
   * @param offset The start index of accounts to return
   * @param listAccountsRequest the list accounts request dto
   * @return the list accounts response dto
   */
  @Override
  public ListAccountsResponseDTO listAccounts(long offset, ListAccountsRequestDTO listAccountsRequest) throws VaultServiceException {
    if(offset > 0) {
      String marker = getAccountsMarker((int)offset, CD_TENANT_ID_PREFIX);
      logger.debug("List Accounts called with marker:{}", marker);
      listAccountsRequest.setMarker(marker);
    }
    ListAccountsResponseDTO listAccountsResponse = listAccounts(listAccountsRequest);
    if(listAccountsResponse.isTruncated()) {
      // Store (offset + listAccountsResponse.getAccounts().size()) as key for the received marker
      cacheListAccountsMarker((int) (offset + listAccountsResponse.getAccounts().size()), listAccountsResponse.getMarker());
    }
    return listAccountsResponse;
  }

  /**
   * Returns account marker
   * <p>This method will return the accounts marker for the provided offset.
   *
   * @param offset The start index of accounts to return
   * @param filterKey the filterkey will for filterKeyStartsWith
   * @return the marker string
   */
  public String getAccountsMarker(int offset, String filterKey) throws VaultServiceException {

    String marker = null;
    if(listAccountsMarkerCache != null){
      marker = listAccountsMarkerCache.get(offset);
    }

    // If marker available in cache
    if(marker  == null){

        // Move index 0 to offset and capture all missing markers
        int index = 0;
        for(;index < offset; index += 1000){

          int maxItems = (offset < 1000) ? offset : 1000;
          int nextOffset = index + maxItems;

          if(listAccountsMarkerCache != null && listAccountsMarkerCache.get(nextOffset) !=null){
            marker = listAccountsMarkerCache.get(nextOffset);
            continue;
          }

          ListAccountsRequestDTO listAccountsRequest = ListAccountsRequestDTO.builder()
                  .maxItems(maxItems)
                  .filterKeyStartsWith(filterKey)
                  .build();

          if(marker != null) {
            listAccountsRequest.setMarker(marker);
          }

          ListAccountsResponseDTO listAccountsResponse = listAccounts(listAccountsRequest);

          if(listAccountsResponse.isTruncated()) {

            marker = listAccountsResponse.getMarker();
            cacheListAccountsMarker(listAccountsResponse.getAccounts().size(), marker);

          } else{
            throw new VaultServiceException(HttpStatus.BAD_REQUEST, "Requested offset is outside the total available items");
          }

        }

    }
    return marker;
  }

  private void cacheListAccountsMarker(int key, String marker) {
    if(listAccountsMarkerCache != null) {
        listAccountsMarkerCache.put(key, marker);
    }
  }

  private void cacheAssumeRoleCredentials(String roleArn, Credentials credentials) {
    if(assumeRoleCache != null) {
      assumeRoleCache.put(roleArn, credentials);
    }
  }

  @Override
  public ListUsersResult listUsers(String accountId, long offset, long limit, AmazonIdentityManagement iamClient) {
    ListUsersRequest listUsersRequest =  new ListUsersRequest()
                                            .withMaxItems((int) limit);
    if(offset > 0) {
      String marker = getUsersMarker((int)offset, accountId, iamClient);
      logger.debug("List Users called with marker:{}", marker);
      listUsersRequest.setMarker(marker);
    }

    logger.info("[Vault] List Users Request:{}", new Gson().toJson(listUsersRequest));

    ListUsersResult listUsersResult = iamClient.listUsers(listUsersRequest);

    logger.info("[Vault] List Users response:{}", new Gson().toJson(listUsersResult));

    if(listUsersResult.isTruncated()) {
      // Store (offset + listUsersResult.getUsers().size()) as key for the received marker
      int markerKey = (int) (offset + listUsersResult.getUsers().size());
      cacheListUsersMarker(getListUsersCacheKey(markerKey, accountId), listUsersResult.getMarker());
    }

    return listUsersResult;
  }

  /**
   * Returns users marker
   * <p>This method will return the users marker for the provided offset.
   *
   * @param offset The start index of users to return
   * @param iamClient the IAM Client
   * @return the marker string
   */
  private String getUsersMarker(int offset, String accountId, AmazonIdentityManagement iamClient) throws VaultServiceException {

    String marker = null;

    if(listUsersMarkerCache != null){
      String offsetKey = getListUsersCacheKey(offset, accountId);
      marker = listUsersMarkerCache.get(offsetKey);
    }

    // If marker available in cache
    if(marker  == null){

      // Move index 0 to offset and capture all missing markers
      for(int index = 0;index < offset; index += 1000){

        int maxItems = (offset < 1000) ? offset : 1000;

        String nextOffset = getListUsersCacheKey((index + maxItems), accountId);

        if(listUsersMarkerCache != null && listUsersMarkerCache.get(nextOffset) !=null){
          marker = listUsersMarkerCache.get(nextOffset);
          continue;
        }

        ListUsersRequest listUsersRequest = new ListUsersRequest().withMaxItems(maxItems);

        if(marker != null) {
          listUsersRequest.setMarker(marker);
        }

        ListUsersResult listUsersResponse = iamClient.listUsers(listUsersRequest);

        if(listUsersResponse.isTruncated()) {

          marker = listUsersResponse.getMarker();
          String key = getListUsersCacheKey(listUsersResponse.getUsers().size(), accountId);
          cacheListUsersMarker(key, marker);

        } else{
          throw new VaultServiceException(HttpStatus.BAD_REQUEST, "Requested offset is outside the total available items");
        }

      }

    }
    return marker;
  }

  private String getListUsersCacheKey(int offset, String accountId) {
    return accountId + "_" + offset;
  }

  private void cacheListUsersMarker(String key, String marker) {
    if(listUsersMarkerCache != null) {
      listUsersMarkerCache.put(key, marker);
    }
  }

  @Override
  public Credentials getTempAccountCredentials(AssumeRoleRequest assumeRoleRequest) {
    Credentials credentials = (assumeRoleCache != null) ? assumeRoleCache.get(assumeRoleRequest.getRoleArn()) : null;

    if(credentials == null) {
      AssumeRoleResult assumeRoleResult = ExternalServiceFactory.executeVaultService(vaultSTSClient::assumeRoleBackbeat, assumeRoleRequest);

      credentials = assumeRoleResult.getCredentials();

      cacheAssumeRoleCredentials(assumeRoleRequest.getRoleArn(), credentials);
    }

    return credentials;
  }

  @Override
  public AmazonIdentityManagement getIAMClient(Credentials credentials, String region) {
    if(StringUtils.isNullOrEmpty(credentials.getSessionToken())) {
      return AmazonIdentityManagementClientBuilder.standard()
              .withCredentials(new AWSStaticCredentialsProvider(
                      new BasicAWSCredentials(
                              credentials.getAccessKeyId(),
                              credentials.getSecretAccessKey())))
              .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(vaultAdminEndpoint, region))
              .build();
    } else{
      return AmazonIdentityManagementClientBuilder.standard()
              .withCredentials(new AWSStaticCredentialsProvider(
                      new BasicSessionCredentials(
                              credentials.getAccessKeyId(),
                              credentials.getSecretAccessKey(),
                              credentials.getSessionToken())))
              .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(vaultAdminEndpoint, region))
              .build();
    }
  }

  /**
   * Returns the temporary account credentials
   *
   * @param generateAccountAccessKeyRequest Generate Account Access Key Request
   * @return GenerateAccountAccessKeyResponse with AK/SK for the account
   */
  @Override
  public GenerateAccountAccessKeyResponse getAccountAccessKey(GenerateAccountAccessKeyRequest generateAccountAccessKeyRequest) {
    return ExternalServiceFactory.executeVaultService(vaultAccountClient::generateAccountAccessKey, generateAccountAccessKeyRequest);
  }

  /**
   * Get account with account id
   * <p>This method will get the account from Vault using Account ID.
   *
   * @param getAccountRequestDTO the get account request dto
   * @return the account
   */
  @Override
  public AccountData getAccountWithID(GetAccountRequestDTO getAccountRequestDTO) {
    return ExternalServiceFactory.executeVaultService(vaultAccountClient::getAccount, getAccountRequestDTO);
  }
}
