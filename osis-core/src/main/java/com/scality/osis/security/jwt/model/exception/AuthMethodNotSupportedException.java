/**
 * Copyright 2020 VMware, Inc.
 * Copyright 2022 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.security.jwt.model.exception;

import org.springframework.security.authentication.AuthenticationServiceException;

/**
 * Thrown when an authentication method is not supported.
 * @see AuthenticationServiceException
 */
public class AuthMethodNotSupportedException extends AuthenticationServiceException {

    /**
     * Constructs an {@code AuthMethodNotSupportedException} with the specified message.
     * @param msg the detail message
     */
    public AuthMethodNotSupportedException(String msg) {
        super(msg);
    }
}
