/**
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.vaultadmin.impl;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Represents Vault admin errors
 *
 * <p>Created by saitharun on 12/24/20.
 */
@SuppressWarnings("SameParameterValue")
public class VaultServiceException extends ResponseStatusException {

  private static final long serialVersionUID = 8292089200348420677L;

  private String errorCode = "";

  public String getErrorCode() {
    return errorCode;
  }

  public VaultServiceException(HttpStatus status, String message) {
    super(status, message);
  }

  public VaultServiceException(HttpStatus status, String errorCode, String message) {
    super(status, message);
    this.errorCode = errorCode;
  }

  public VaultServiceException(HttpStatus status, String messageCode, Throwable cause) {
    super(status, messageCode, cause);
  }
}
