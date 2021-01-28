/**
 *Copyright 2020 VMware, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.vaultadmin;

import com.scality.vaultclient.dto.CreateAccountRequestDTO;
import com.scality.vaultclient.dto.CreateAccountResponseDTO;

/**
 * Vault administrator
 *
 * <p>Administer the Scality Object Storage (a.k.a. Vault) service with user management, access
 * controls, quotas and usage tracking among other features.
 *
 * <p>Note that to some operations needs proper configurations on vault, and require that the
 * requester holds special administrative capabilities.
 *
 * <p>Created by saitharun on 3/14/17.
 */
@SuppressWarnings("SameParameterValue")
public interface VaultAdmin {
  CreateAccountResponseDTO createAccount(CreateAccountRequestDTO accountRequest);
}
