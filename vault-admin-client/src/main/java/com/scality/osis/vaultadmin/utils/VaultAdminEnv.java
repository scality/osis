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
    public Integer getListAccountsCacheMaxCapacity(){
       return StringUtils.isEmpty(env.getProperty(CacheConstants.ENV_LIST_ACCOUNT_MAX_CAPACITY))
                ? null : Integer.parseInt(env.getProperty(CacheConstants.ENV_LIST_ACCOUNT_MAX_CAPACITY));
    }

    /**
     * Get list accounts cache expiration time.
     *
     * @return the integer
     */
    public Long getListAccountsCacheExpiration(){
        return StringUtils.isEmpty(env.getProperty(CacheConstants.ENV_LIST_ACCOUNT_CACHE_TTL))
                ? null : Long.parseLong(env.getProperty(CacheConstants.ENV_LIST_ACCOUNT_CACHE_TTL));
    }

    /**
     * Get list accounts max capacity integer.
     *
     * @return the integer
     */
    public Integer getAssumeRoleCacheMaxCapacity(){
        return StringUtils.isEmpty(env.getProperty(CacheConstants.ENV_ASSUME_ROLE_MAX_CAPACITY))
                ? null : Integer.parseInt(env.getProperty(CacheConstants.ENV_ASSUME_ROLE_MAX_CAPACITY));
    }

    /**
     * Get list accounts cache expiration time.
     *
     * @return the integer
     */
    public Long getAssumeRoleCacheExpiration(){
        return StringUtils.isEmpty(env.getProperty(CacheConstants.ENV_ASSUME_ROLE_CACHE_TTL))
                ? null : Long.parseLong(env.getProperty(CacheConstants.ENV_ASSUME_ROLE_CACHE_TTL));
    }

    /**
     * Is list users cache disabled boolean.
     *
     * @return the boolean
     */
    public boolean isListUsersCacheDisabled(){
        return Boolean.parseBoolean(env.getProperty(CacheConstants.ENV_LIST_USERS_DISABLED));
    }

    /**
     * Get list users max capacity integer.
     *
     * @return the integer
     */
    public Integer getListUsersCacheMaxCapacity(){
        return StringUtils.isEmpty(env.getProperty(CacheConstants.ENV_LIST_USERS_MAX_CAPACITY))
                ? null : Integer.parseInt(env.getProperty(CacheConstants.ENV_LIST_USERS_MAX_CAPACITY));
    }

    /**
     * Get list users cache expiration time.
     *
     * @return the integer
     */
    public Long getListUsersCacheExpiration(){
        return StringUtils.isEmpty(env.getProperty(CacheConstants.ENV_LIST_USERS_CACHE_TTL))
                ? null : Long.parseLong(env.getProperty(CacheConstants.ENV_LIST_USERS_CACHE_TTL));
    }

}
