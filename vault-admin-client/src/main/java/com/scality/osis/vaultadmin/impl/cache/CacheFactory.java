/**
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.vaultadmin.impl.cache;

import com.scality.osis.vaultadmin.utils.VaultAdminEnv;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.scality.osis.vaultadmin.impl.cache.CacheConstants.*;

/**
 * The factory class for all Caches.
 */
@Component
public class CacheFactory {

    @Autowired
    private VaultAdminEnv env;

    private Cache<Integer, String> listAccountsMarkerCache;

    private CacheFactory(){

        initListAccountsMarkerCache();

    }

    /**
     * Instantiates a new Cache factory.
     *
     * @param env the env
     */
    public CacheFactory(VaultAdminEnv env){
        this.env =env;
        initListAccountsMarkerCache();

    }

    private void initListAccountsMarkerCache() {
        // if listAccount cache not disabled
        if(!env.isListAccountsCacheDisabled()) {
            int maxCapacity = env.getListAccountsMaxCapacity() !=null
                    ? env.getListAccountsMaxCapacity() : DEFAULT_CACHE_MAX_CAPACITY;
            listAccountsMarkerCache = new CacheImpl<>(maxCapacity);
        }
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
        }
        return null;
    }

}
