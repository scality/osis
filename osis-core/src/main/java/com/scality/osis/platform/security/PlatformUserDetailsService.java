/**
 * Copyright 2020 VMware, Inc.
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.platform.security;

import com.scality.osis.platform.AppEnv;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class PlatformUserDetailsService implements UserDetailsService {

    @Autowired
    private AppEnv appEnv;

    @Override
    public UserDetails loadUserByUsername(String username) {
        if (username == null || !username.equals(appEnv.getPlatformAccessKey())) {
            throw new UsernameNotFoundException(username);
        }
        return new PlatformUserDetails(appEnv.getPlatformAccessKey(), appEnv.getPlatformSecretKey());
    }
}
