/**
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.vaultadmin.impl;

import com.scality.vaultclient.services.VaultClientException;
import com.amazonaws.http.HttpResponse;

class ErrorUtils {

  static VaultServiceException parseError(HttpResponse response) {
      return new VaultServiceException(
              response.getStatusCode(), response.getStatusText());
  }

  static VaultServiceException parseError(VaultClientException vaultClientException) {
      return new VaultServiceException(
              vaultClientException.getStatusCode(), vaultClientException.getErrorCode());
  }

  public static boolean isSuccessful(int statusCode) {
    return statusCode >= 200 && statusCode < 300;
  }
}
