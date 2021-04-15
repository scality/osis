/**
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.vaultadmin.impl;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.scality.osis.vaultadmin.impl.cache.Cache;
import com.scality.osis.vaultadmin.impl.cache.CacheConstants;
import com.scality.osis.vaultadmin.impl.cache.CacheFactory;
import com.scality.vaultclient.dto.AssumeRoleResult;
import com.scality.vaultclient.dto.CreateAccountRequestDTO;
import com.scality.vaultclient.dto.CreateAccountResponseDTO;
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

/**
 * Vault administrator implementation
 *
 * <p>Created by saitharun on 12/16/20.
 */
@SuppressWarnings({"deprecation", "unchecked"})
public class VaultAdminImpl implements VaultAdmin{
  private static final Logger logger = LoggerFactory.getLogger(VaultAdminImpl.class);

  public static final String CD_TENANT_ID_PREFIX = "cd_tenant_id";

  private final AccountServicesClient vaultAccountClient;

  private final SecurityTokenServicesClient vaultSTSClient;

  @Autowired
  private CacheFactory cacheFactory;

  private Cache<Integer, String> listAccountsMarkerCache;

  private Cache<String, Credentials> assumeRoleCache;

  /**
   * Create a Vault administrator implementation
   *  @param accessKey Access key of the admin who have proper administrative capabilities.
   * @param secretKey Secret key of the admin who have proper administrative capabilities.
   * @param adminEndpoint Vault admin API endpoint, e.g., https://127.0.0.1:8600
   * @param s3InterfaceEndpoint Vault S3 Interface endpoint, e.g., https://127.0.0.1:8500
   */
  public VaultAdminImpl(String accessKey, String secretKey, String adminEndpoint, String s3InterfaceEndpoint) {
    validEndpoint(adminEndpoint);
    this.vaultAccountClient = new AccountServicesClient(
            new BasicAWSCredentials(accessKey, secretKey));
    vaultAccountClient.setEndpoint(adminEndpoint);


    this.vaultSTSClient = new SecurityTokenServicesClient(
            new BasicAWSCredentials(accessKey, secretKey));
    vaultSTSClient.setEndpoint(s3InterfaceEndpoint);

    initCaches();
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
    this.vaultAccountClient = vaultAccountClient;
    vaultAccountClient.setEndpoint(adminEndpoint);

    validEndpoint(s3InterfaceEndpoint);
    this.vaultSTSClient = vaultSTSClient;
    vaultSTSClient.setEndpoint(s3InterfaceEndpoint);

    initCaches();
  }

  public void initCaches() {
    if(cacheFactory !=null) {
      listAccountsMarkerCache = cacheFactory.getCache(CacheConstants.NAME_LIST_ACCOUNTS_CACHE);
      assumeRoleCache = cacheFactory.getCache(CacheConstants.NAME_ASSUME_ROLE_CACHE);
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
            throw new VaultServiceException(400, "Requested offset is outside the total available items");
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
  public Credentials getTempAccountCredentials(AssumeRoleRequest assumeRoleRequest) {
    Credentials credentials = (assumeRoleCache != null) ? assumeRoleCache.get(assumeRoleRequest.getRoleArn()) : null;

    if(credentials == null) {
      AssumeRoleResult assumeRoleResult = ExternalServiceFactory.executeVaultService(vaultSTSClient::assumeRoleBackbeat, assumeRoleRequest);

      credentials = assumeRoleResult.getCredentials();

      cacheAssumeRoleCredentials(assumeRoleRequest.getRoleArn(), credentials);
    }

    return credentials;
  }
}
