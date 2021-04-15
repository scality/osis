/**
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.vaultadmin.impl.cache;

import com.amazonaws.services.securitytoken.model.Credentials;
import com.scality.osis.vaultadmin.utils.VaultAdminEnv;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static com.scality.osis.vaultadmin.impl.cache.CacheConstants.*;

/**
 * The factory class for all Caches.
 */
@Component
public class CacheFactory {

    @Autowired
    private VaultAdminEnv env;

    private Cache<Integer, String> listAccountsMarkerCache;

    private Cache<String, Credentials> assumeRoleCache;

    private CacheFactory(){

    }

    /**
     * Instantiates a new Cache factory.
     *
     * @param env the env
     */
    public CacheFactory(VaultAdminEnv env){
        this.env =env;
        initListAccountsMarkerCache();
        initAssumeRoleCache();
    }

    @PostConstruct
    private void initListAccountsMarkerCache() {
        // if listAccount cache not disabled
        if(!env.isListAccountsCacheDisabled()) {
            int maxCapacity = env.getListAccountsCacheMaxCapacity() !=null
                    ? env.getListAccountsCacheMaxCapacity() : DEFAULT_CACHE_MAX_CAPACITY;

            long expirationTime = env.getListAccountsCacheExpiration() !=null
                    ? env.getListAccountsCacheExpiration() : DEFAULT_CACHE_MAX_CAPACITY;
            listAccountsMarkerCache = new CacheImpl<>(maxCapacity, expirationTime);
        }
    }

    @PostConstruct
    private void initAssumeRoleCache() {
        int maxCapacity = env.getAssumeRoleCacheMaxCapacity() !=null
                ? env.getAssumeRoleCacheMaxCapacity() : DEFAULT_CACHE_MAX_CAPACITY;

        long expirationTime = env.getAssumeRoleCacheExpiration() !=null
                ? env.getAssumeRoleCacheExpiration() : DEFAULT_CACHE_MAX_CAPACITY;
        assumeRoleCache = new CacheImpl<>(maxCapacity, expirationTime);
    }

    /**
     * Get cache object using cache name.
     *
     * @param cacheName the cache name
     * @return the cache object
     */
    public Cache getCache(String cacheName){
        switch(cacheName){
            case NAME_LIST_ACCOUNTS_CACHE : return listAccountsMarkerCache;
            case NAME_ASSUME_ROLE_CACHE : return assumeRoleCache;
        }
        return null;
    }

    public VaultAdminEnv getEnvironmentVariables(){
        return env;
    }

}
