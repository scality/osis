/**
 *Copyright 2020 VMware, Inc.
 *Copyright 2022 Scality, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.security.jwt.model;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * This class represents an authentication token that is used to authenticate a user.
 * It extends the Spring AbstractAuthenticationToken class and provides implementations for its abstract methods.
 * @see AbstractAuthenticationToken
 * @see RawAccessToken
 * @see UserContext
 */
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private transient RawAccessToken rawAccessToken;
    private transient UserContext userContext;

    /**
     * Creates an authentication token with the specified raw access token.
     * @param unsafeToken the raw access token
     */
    public JwtAuthenticationToken(RawAccessToken unsafeToken) {
        super(null);
        this.rawAccessToken = unsafeToken;
        this.setAuthenticated(false);
    }

    /**
     * Creates an authentication token with the specified user context and authorities.
     * @param userContext the user context
     * @param authorities the authorities
     */
    public JwtAuthenticationToken(UserContext userContext, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.eraseCredentials();
        this.userContext = userContext;
        super.setAuthenticated(true);
    }

    @Override
    public void setAuthenticated(boolean authenticated) {
        if (authenticated) {
            throw new IllegalArgumentException(
                    "The token is not trusted. There should be GrantedAuthority list");
        }
        super.setAuthenticated(false);
    }

    @Override
    public Object getCredentials() {
        return rawAccessToken;
    }

    @Override
    public Object getPrincipal() {
        return this.userContext;
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        this.rawAccessToken = null;
    }

    @Override
    public boolean equals(Object obj) {
        return !ObjectUtils.notEqual(this, obj);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.rawAccessToken).append(this.userContext).build();
    }
}
