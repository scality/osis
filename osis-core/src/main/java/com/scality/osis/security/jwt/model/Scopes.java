/**
 *Copyright 2020 VMware, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.vmware.osis.security.jwt.model;

public enum Scopes {
    REFRESH_TOKEN;

    String authority;

    Scopes() {
        this.authority = "ROLE_" + this.name();
    }

    public String authority() {
        return authority;
    }
}
