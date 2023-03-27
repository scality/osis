/**
 *Copyright 2020 VMware, Inc.
 *Copyright 2022 Scality, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.security.jwt.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.jsonwebtoken.Claims;

/**
 * Represents a JWT access token with associated claims.
 * This class is used in conjunction with JWT-based authentication in the Scality OSIS API.
 * @see JwtToken
 */
public class AccessToken implements JwtToken {
    private final String rawToken;

    @JsonIgnore
    private Claims claims;

    /**
     * Constructs a new access token.
     * @param token the JWT string
     * @param claims the claims associated with the token (JWT)
     */
    public AccessToken(final String token, Claims claims) {
        this.rawToken = token;
        this.claims = claims;
    }

    /**
     * @return the raw JWT string
     */
    public String getToken() {
        return this.rawToken;
    }

    /**
     * @return the claims associated with the token (JWT)
     */
    public Claims getClaims() {
        return claims;
    }
}
