/**
 *Copyright 2020 VMware, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.vmware.osis.security.jwt.verifier;

public interface JwtTokenVerifier {
    boolean verify(String jti);
}
