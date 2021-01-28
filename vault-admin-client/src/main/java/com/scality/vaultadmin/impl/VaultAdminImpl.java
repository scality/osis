/**
 *Copyright 2020 VMware, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.vaultadmin.impl;

import com.amazonaws.auth.BasicAWSCredentials;
import com.scality.vaultclient.dto.CreateAccountRequestDTO;
import com.scality.vaultclient.dto.CreateAccountResponseDTO;
import com.scality.vaultclient.services.AccountServicesClient;
import com.scality.vaultclient.services.VaultClientException;
import okhttp3.*;
import com.scality.vaultadmin.VaultAdmin;

/**
 * Vault administrator implementation
 *
 * <p>Created by saitharun on 2/16/17.
 */
public class VaultAdminImpl implements VaultAdmin {

  private final String endpoint;
  private final AccountServicesClient vaultAccountclient;

  /**
   * Create a Vault administrator implementation
   *  @param accessKey Access key of the admin who have proper administrative capabilities.
   * @param secretKey Secret key of the admin who have proper administrative capabilities.
   * @param endpoint Vault admin API endpoint, e.g., http://127.0.0.1:80/admin
   */
  public VaultAdminImpl(String accessKey, String secretKey, String endpoint) {
    validEndpoint(endpoint);
    this.vaultAccountclient = new AccountServicesClient(
            new BasicAWSCredentials(accessKey, secretKey));
    this.endpoint = endpoint;
    vaultAccountclient.setEndpoint(endpoint);
//    AwsClientBuilder.EndpointConfiguration endpointConfig = new AwsClientBuilder.EndpointConfiguration(endpoint, "");
  }

  /**
   * Create a Vault administrator implementation
   * @param vaultAccountclient Vault Account Service client.
   * @param endpoint Vault admin API endpoint, e.g., http://127.0.0.1:80/admin
   */
  public VaultAdminImpl(AccountServicesClient vaultAccountclient, String endpoint) {
    validEndpoint(endpoint);
    this.vaultAccountclient = vaultAccountclient;
    this.endpoint = endpoint;
    vaultAccountclient.setEndpoint(endpoint);
  }

  private static void validEndpoint(String endpoint) {
    if (HttpUrl.parse(endpoint) == null) {
      throw new IllegalArgumentException("endpoint is invalid");
    }
  }

  @Override
  public CreateAccountResponseDTO createAccount(CreateAccountRequestDTO accountRequest) {
    try {
      com.amazonaws.Response<CreateAccountResponseDTO> response = vaultAccountclient.createAccount(accountRequest);
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
}
