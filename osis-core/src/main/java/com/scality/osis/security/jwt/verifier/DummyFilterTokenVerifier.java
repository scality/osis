/**
 *Copyright 2020 VMware, Inc.
 *Copyright 2021 Scality, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.security.jwt.verifier;

import org.springframework.stereotype.Component;

@Component
public class DummyFilterTokenVerifier implements JwtTokenVerifier {
    @Override
    public boolean verify(String jti) {
        return true;
    }
}
