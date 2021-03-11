package com.scality.osis.security.jwt.model;

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
