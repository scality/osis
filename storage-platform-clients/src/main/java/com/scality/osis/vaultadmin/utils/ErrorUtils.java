/**
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.vaultadmin.utils;

import com.scality.osis.vaultadmin.impl.VaultServiceException;
import com.scality.vaultclient.services.VaultClientException;
import com.amazonaws.http.HttpResponse;
import org.springframework.http.HttpStatus;

public final class ErrorUtils {
  private ErrorUtils(){

  }

  public static VaultServiceException parseError(HttpResponse response) {
      return new VaultServiceException(
              HttpStatus.valueOf(response.getStatusCode()), response.getStatusText());
  }

  public static VaultServiceException parseError(VaultClientException vaultClientException) {
      return new VaultServiceException(
              HttpStatus.valueOf(vaultClientException.getStatusCode()), vaultClientException.getErrorCode(), vaultClientException.getErrorMessage());
  }

  public static boolean isSuccessful(int statusCode) {
    return statusCode >= 200 && statusCode < 300;
  }
}
