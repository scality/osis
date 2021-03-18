/**
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.vaultadmin.impl;

import com.amazonaws.auth.BasicAWSCredentials;
import com.scality.vaultclient.dto.CreateAccountRequestDTO;
import com.scality.vaultclient.dto.CreateAccountResponseDTO;
import com.scality.vaultclient.services.AccountServicesClient;
import com.scality.vaultclient.services.VaultClientException;
import okhttp3.*;
import com.scality.osis.vaultadmin.VaultAdmin;

/**
 * Vault administrator implementation
 *
 * <p>Created by saitharun on 12/16/20.
 */
@SuppressWarnings("deprecation")
public class VaultAdminImpl implements VaultAdmin {

  private final AccountServicesClient vaultAccountClient;

  /**
   * Create a Vault administrator implementation
   *  @param accessKey Access key of the admin who have proper administrative capabilities.
   * @param secretKey Secret key of the admin who have proper administrative capabilities.
   * @param endpoint Vault admin API endpoint, e.g., http://127.0.0.1:80/admin
   */
  public VaultAdminImpl(String accessKey, String secretKey, String endpoint) {
    validEndpoint(endpoint);
    this.vaultAccountClient = new AccountServicesClient(
            new BasicAWSCredentials(accessKey, secretKey));
    vaultAccountClient.setEndpoint(endpoint);
  }

  /**
   * Create a Vault administrator implementation
   * @param vaultAccountClient Vault Account Service client.
   * @param endpoint Vault admin API endpoint, e.g., http://127.0.0.1:80/admin
   */
  public VaultAdminImpl(AccountServicesClient vaultAccountClient, String endpoint) {
    validEndpoint(endpoint);
    this.vaultAccountClient = vaultAccountClient;
    vaultAccountClient.setEndpoint(endpoint);
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
   */
  @Override
  public CreateAccountResponseDTO createAccount(CreateAccountRequestDTO createAccountRequest) {
    try {
      com.amazonaws.Response<CreateAccountResponseDTO> response = vaultAccountClient.createAccount(createAccountRequest);
      if (null!= response.getHttpResponse() && ErrorUtils.isSuccessful(response.getHttpResponse().getStatusCode())) {
        return response.getAwsResponse();
      } else {
        throw ErrorUtils.parseError(response.getHttpResponse());
      }
    } catch (VaultServiceException e) {
      throw e;
    }catch (VaultClientException e){
      throw ErrorUtils.parseError(e);
    } catch (Exception e) {
      throw new VaultServiceException(500, "Exception", e);
    }
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
}
