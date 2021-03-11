/**
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.vaultadmin.impl;

/**
 * Represents Vault admin errors
 *
 * <p>Created by saitharun on 12/24/20.
 */
@SuppressWarnings("SameParameterValue")
public class VaultServiceException extends RuntimeException {

  private static final long serialVersionUID = 8292089200348420677L;
  private final int statusCode;

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
