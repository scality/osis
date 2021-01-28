/**
 *Copyright 2020 VMware, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.vaultadmin.impl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.scality.vaultclient.services.VaultClientException;
import okhttp3.Response;
import com.amazonaws.http.HttpResponse;

import java.lang.reflect.Type;
import java.util.Map;

/** Created by saitharun on 3/24/17. */
class ErrorUtils {
  private static final Type mapType = new TypeToken<Map<String, String>>() {}.getType();
  private static final Gson gson = new Gson();

  static VaultServiceException parseError(Response response) {
    try {
      //noinspection unchecked
      return new VaultServiceException(
          response.code(),
          ((Map<String, String>) gson.fromJson(response.body().string(), mapType)).get("Code"));
    } catch (Exception e) {
      return new VaultServiceException(response.code());
    }
  }

  static VaultServiceException parseError(HttpResponse response) {
    try {
      //noinspection unchecked
      return new VaultServiceException(
              response.getStatusCode(), response.getStatusText());
    } catch (Exception e) {
      return new VaultServiceException(response.getStatusCode());
    }
  }

  static VaultServiceException parseError(VaultClientException vaultClientException) {
    try {
      //noinspection unchecked
      return new VaultServiceException(
              vaultClientException.getStatusCode(), vaultClientException.getErrorCode());
    } catch (Exception e) {
      return new VaultServiceException(vaultClientException.getStatusCode());
    }
  }

  public static boolean isSuccessful(int statusCode) {
    return statusCode >= 200 && statusCode < 300;
  }
}
