/**
 *Copyright 2020 VMware, Inc.
 *Copyright 2022 Scality, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.security.jwt.verifier;

/**
 * Interface for verifying a JWT token.
 */
public interface JwtTokenVerifier {

    /**
     * Verify a JWT token.
     *
     * @param jti the JWT token to verify
     * @return true if the token is valid, false otherwise
     */
    boolean verify(String jti);
}
