/**
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.vaultadmin;

import com.scality.vaultclient.dto.CreateAccountRequestDTO;
import com.scality.vaultclient.dto.CreateAccountResponseDTO;
import com.scality.vaultclient.services.AccountServicesClient;

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
}
