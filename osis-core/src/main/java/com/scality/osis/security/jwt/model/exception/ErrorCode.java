/**
 *Copyright 2020 VMware, Inc.
 *Copyright 2022 Scality, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */
package com.scality.osis.security.jwt.model.exception;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Error codes for authentication failures
 */
public enum ErrorCode {

    /**
     * This is an enumeration that defines two constant values AUTHENTICATION and JWT_TOKEN_EXPIRED,
     * with their respective integer values 1 and 2. These constants are used to represent the error
     * codes in the ErrorResponse class. AUTHENTICATION is used to indicate errors related to
     * authentication, and JWT_TOKEN_EXPIRED is used to indicate errors related to JWT token
     * expiration.
     */
    AUTHENTICATION(1), JWT_TOKEN_EXPIRED(2);

    /**
     * The error code
     */
    private int code;

    /**
     * Constructs a new error code
     *
     * @param code the error code
     */
    ErrorCode(int code) {
        this.code = code;
    }

    /**
     * Get the error code
     *
     * @return the error code
     */
    @JsonValue
    public int getCode() {
        return code;
    }
}
