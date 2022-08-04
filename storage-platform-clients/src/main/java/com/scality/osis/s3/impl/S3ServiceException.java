/**
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.s3.impl;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Represents S3 errors
 *
 */
@SuppressWarnings("SameParameterValue")
public class S3ServiceException extends ResponseStatusException {

  private static final long serialVersionUID = 8292089200348420677L;

  private String errorCode = "";

  public String getErrorCode() {
    return errorCode;
  }

  public S3ServiceException(HttpStatus status, String message) {
    super(status, message);
  }

  public S3ServiceException(HttpStatus status, String errorCode, String message) {
    super(status, message);
    this.errorCode = errorCode;
  }

  public S3ServiceException(HttpStatus status, String messageCode, Throwable cause) {
    super(status, messageCode, cause);
  }
}
