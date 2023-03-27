/**
 *Copyright 2020 VMware, Inc.
 *Copyright 2022 Scality, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.security.jwt.extractor;

/**
 * Interface for extracting a JWT token from a payload.
 */
public interface JwtTokenExtractor {

    /**
     * Extract a JWT token from a payload.
     *
     * @param payload the payload
     * @return the JWT token
     */
    String extract(String payload);
}
