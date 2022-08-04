/**
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.vaultadmin;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.scality.vaultclient.dto.AccountData;
import com.scality.vaultclient.dto.CreateAccountRequestDTO;
import com.scality.vaultclient.dto.CreateAccountResponseDTO;
import com.scality.vaultclient.dto.GenerateAccountAccessKeyRequest;
import com.scality.vaultclient.dto.GenerateAccountAccessKeyResponse;
import com.scality.vaultclient.dto.GetAccountRequestDTO;
import com.scality.vaultclient.dto.ListAccountsRequestDTO;
import com.scality.vaultclient.dto.ListAccountsResponseDTO;
import com.scality.vaultclient.dto.UpdateAccountAttributesRequestDTO;
import com.scality.vaultclient.services.AccountServicesClient;
import com.scality.vaultclient.services.SecurityTokenServicesClient;

/**
 * Vault administrator
 *
 * <p>Administer the Scality Object Storage (a.k.a. Vault) service with user management, access
 * controls, quotas and usage tracking among other features.
 *
 * <p>Note that to some operations needs proper configurations on vault, and require that the
 * requester holds special administrative capabilities.
 *
 * <p>Created by saitharun on 12/10/20.
 */
@SuppressWarnings("SameParameterValue")
public interface VaultAdmin {
  /**
   * Create a new Account
   *
   * <p>This method will create the account on Vault.
   *
   * @param createAccountRequest CreateAccountRequestDTO object with account name, account externalAccountId.
   * @return The created account response object.
   */
  CreateAccountResponseDTO createAccount(CreateAccountRequestDTO createAccountRequest);
  /**
   * Returns the vault client to invoke account services
   *
   * @return The vault client object.
   */
  AccountServicesClient getVaultAccountclient();
  /**
   * Returns the vault client to invoke account services
   *
   * @return The vault client object.
   */
  SecurityTokenServicesClient getVaultSTSclient();

  /**
   * List accounts
   *
   * @param listAccountsRequest the list accounts request dto
   * @return the list accounts response dto
   */
  ListAccountsResponseDTO listAccounts(ListAccountsRequestDTO listAccountsRequest);

  /**
   * List accounts
   *
   * @param offset the start index of accounts to return
   * @param listAccountsRequest the list accounts request dto
   * @return the list accounts response dto
   */
  ListAccountsResponseDTO listAccounts(long offset, ListAccountsRequestDTO listAccountsRequest);

  /**
   * Returns the temporary account credentials
   *
   * @param assumeRoleRequest the assumeRoleRequest dto with a role arn
   * @return the credentials object with temporary access key and secret key of the account
   */
  Credentials getTempAccountCredentials(AssumeRoleRequest assumeRoleRequest);

  /**
   * Returns the temporary account credentials
   *
   * @param credentials the credential object with AK/SK and @optional `SessionToken`
   * @param region the region
   * @return the iam client with requested credentials, vault end point and region
   */
  AmazonIdentityManagement getIAMClient(Credentials credentials, String region);

  /**
   * Returns the temporary account credentials
   *
   * @param generateAccountAccessKeyRequest Generate Account Access Key Request
   * @return GenerateAccountAccessKeyResponse with AK/SK for the account
   */
  GenerateAccountAccessKeyResponse getAccountAccessKey(GenerateAccountAccessKeyRequest generateAccountAccessKeyRequest);

  /**
   * Get account with account id
   * <p>This method will get the account from Vault using Account ID.
   *
   * @param getAccountRequestDTO the get account request dto
   * @return the account
   */
  AccountData getAccount(GetAccountRequestDTO getAccountRequestDTO);

  /**
   * Searches all accounts and returns Vault accountID w.r.t cdTenantId filter key.
   * @param listAccountsRequest with cdTenantIdFilter as filterKey
   *
   * @return the accountId
   */
  String getAccountID(ListAccountsRequestDTO listAccountsRequest);

  /**
   * Update account's customAttributes
   * <p>This method will override the existing custom attributes of an account from Vault identified using Account Name.
   *
   * @param updateAccountAttributesRequestDTO the update account attributes request dto
   * @return The updated account as a create account response object.
   */
  CreateAccountResponseDTO updateAccountAttributes(UpdateAccountAttributesRequestDTO updateAccountAttributesRequestDTO);
}
