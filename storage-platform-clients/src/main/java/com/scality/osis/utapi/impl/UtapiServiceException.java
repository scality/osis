/**
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.utapi.impl;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Represents Utapi errors
 *
 */
@SuppressWarnings("SameParameterValue")
public class UtapiServiceException extends ResponseStatusException {

  private static final long serialVersionUID = 8292089200348420677L;

  private String errorCode = "";

  public String getErrorCode() {
    return errorCode;
  }

  public UtapiServiceException(HttpStatus status, String message) {
    super(status, message);
  }

  public UtapiServiceException(HttpStatus status, String errorCode, String message) {
    super(status, message);
    this.errorCode = errorCode;
  }

  public UtapiServiceException(HttpStatus status, String messageCode, Throwable cause) {
    super(status, messageCode, cause);
  }
}
