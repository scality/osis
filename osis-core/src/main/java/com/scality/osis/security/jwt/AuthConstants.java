/**
 *Copyright 2020 VMware, Inc.
 *Copyright 2022 Scality, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.security.jwt;

public final class AuthConstants {

    private AuthConstants() {
    }

    public static final String API_INFO = "/api/info";

    public static final String CLAIMS_SCOPES = "scopes";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    public static final String KEY_ACCESS_TOKEN = "access_token";
    public static final String KEY_REFRESH_TOKEN = "refresh_token";

    public static final String HEADER_PREFIX = "Bearer ";


}
