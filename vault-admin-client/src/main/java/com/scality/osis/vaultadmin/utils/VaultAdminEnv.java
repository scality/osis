/**
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.vaultadmin.utils;

import com.scality.osis.vaultadmin.impl.cache.CacheConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Class for Vault admin environment variables.
 */
@Component
public class VaultAdminEnv {
    /**
     * Instantiates a new Vault admin env.
     */
    public VaultAdminEnv(){

    }

    @Autowired
    private Environment env;

    /**
     * Is list accounts cache disabled boolean.
     *
     * @return the boolean
     */
    public boolean isListAccountsCacheDisabled(){
        return Boolean.parseBoolean(env.getProperty(CacheConstants.ENV_LIST_ACCOUNT_DISABLED));
    }

    /**
     * Get list accounts max capacity integer.
     *
     * @return the integer
     */
    public Integer getListAccountsMaxCapacity(){
       return StringUtils.isEmpty(env.getProperty(CacheConstants.ENV_LIST_ACCOUNT_MAX_CAPACITY))
                ? null : Integer.parseInt(env.getProperty(CacheConstants.ENV_LIST_ACCOUNT_MAX_CAPACITY));
    }

}
