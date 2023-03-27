/**
 *Copyright 2020 VMware, Inc.
 *Copyright 2022 Scality, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.security.jwt.model;
/**
 * Represents a JSON Web Token (JWT) in the Scality OSIS API, with a method to get the token as a string.
 */
public interface JwtToken {

    /**
     * Get the token as a string.
     *
     * @return the token
     */
    String getToken();
}
