/**
 *Copyright 2020 VMware, Inc.
 *Copyright 2021 Scality, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.platform.security;

import com.scality.osis.security.jwt.AuthConstants;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Collection;

public class ScalityUserDetails implements UserDetails {

    private final String username;
    private final String password;

    public ScalityUserDetails(String username, String password) {
        this.username = username;
        this.password = new BCryptPasswordEncoder().encode(password);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return AuthorityUtils.commaSeparatedStringToAuthorityList(AuthConstants.ROLE_ADMIN);
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
