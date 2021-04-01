/**
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.vaultadmin.impl.cache;

public final class CacheConstants {
    //Env variables
    public static final String ENV_LIST_ACCOUNT_DISABLED = "osis.scality.vault.cache.listAccounts.disabled";
    public static final String ENV_LIST_ACCOUNT_MAX_CAPACITY = "osis.scality.vault.cache.listAccounts.maxCapacity";



    // Defaults
    public static final int DEFAULT_CACHE_MAX_CAPACITY = 1000;
    public static final long DEFAULT_EXPIRY_TIME_IN_MS = 30000;
    public static final int DEFAULT_SCHEDULED_THREAD_POOL_SIZE = 3;

    //Constants
    public static final String NAME_LIST_ACCOUNTS_CACHE = "listAccounts";

    private CacheConstants(){}

}
