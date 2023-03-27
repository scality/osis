/**
 *Copyright 2020 VMware, Inc.
 *Copyright 2022 Scality, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.security.jwt.model.exception;

import com.scality.osis.security.jwt.model.JwtToken;
import org.springframework.security.core.AuthenticationException;

/**
 * This class represents an exception that is thrown when an expired JWT token is encountered.
 */
public class JwtExpiredTokenException extends AuthenticationException {

    private transient JwtToken token;

    /**
     * Constructs a {@code JwtExpiredTokenException} with the specified message.
     *
     * @param msg the detail message
     */
    public JwtExpiredTokenException(String msg) {
        super(msg);
    }

    /**
     * Constructs a {@code JwtExpiredTokenException} with the specified message and root cause.
     * 
     * @param token the expired token
     * @param msg the detail message
     * @param t root cause
     */
    public JwtExpiredTokenException(JwtToken token, String msg, Throwable t) {
        super(msg, t);
        this.token = token;
    }

    /**
     * Returns the token that was expired.
     * @return the token that was expired.
     */
    public String token() {
        return this.token.getToken();
    }
}
