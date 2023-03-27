/**
 * Copyright 2020 VMware, Inc.
 * Copyright 2022 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.security.jwt;

/**
 * Constants used in the JWT authentication process.
 */
public final class AuthConstants {

    /**
     * Private constructor to prevent instantiation of this utility class.
     * It contains static fields for API endpoints, JWT claim names, and other authentication-related values.
     */
    private AuthConstants() {
    }

    /**
     * API path for accessing the API info.
     */
    public static final String API_INFO = "/api/info";

    /**
     * API path for accessing the OpenAPI specification.
     */
    public static final String OPEN_API = "/v3/api-docs/**";

    /**
     * API path for accessing the Swagger UI.
     */
    public static final String SWAGGER = "/swagger*/**";

    /**
     * The name of the JWT claim that contains the user's scopes.
     */
    public static final String CLAIMS_SCOPES = "scopes";

    /**
     * The name of the role that grants administrative privileges.
     */
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    /**
     * The key used to store the access token in the JWT.
     */
    public static final String KEY_ACCESS_TOKEN = "access_token";

    /**
     * The key used to store the refresh token in the JWT.
     */
    public static final String KEY_REFRESH_TOKEN = "refresh_token";

    /**
     * The prefix used in the Authorization header for JWTs.
     */
    public static final String HEADER_PREFIX = "Bearer ";
}
