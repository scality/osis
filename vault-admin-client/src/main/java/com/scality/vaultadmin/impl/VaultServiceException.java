/**
 *Copyright 2020 VMware, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.vaultadmin.impl;

/**
 * Represents Vault admin errors
 *
 * <p>Created by saitharun on 3/24/17.
 */
@SuppressWarnings("SameParameterValue")
public class VaultServiceException extends RuntimeException {
  private final int statusCode;

  public VaultServiceException(int statusCode) {
    this.statusCode = statusCode;
  }

  public VaultServiceException(int statusCode, String messageCode) {
    super(messageCode);
    this.statusCode = statusCode;
  }

  public VaultServiceException(int statusCode, String messageCode, Throwable cause) {
    super(messageCode, cause);
    this.statusCode = statusCode;
  }

  public int status() {
    return statusCode;
  }
}
