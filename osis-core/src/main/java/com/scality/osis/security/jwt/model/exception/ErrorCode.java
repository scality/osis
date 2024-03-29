/**
 *Copyright 2020 VMware, Inc.
 *Copyright 2022 Scality, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */
package com.scality.osis.security.jwt.model.exception;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ErrorCode {

    AUTHENTICATION(1), JWT_TOKEN_EXPIRED(2);

    private int code;

    ErrorCode(int code) {
        this.code = code;
    }

    @JsonValue
    public int getCode() {
        return code;
    }
}
