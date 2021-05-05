/**
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.vaultadmin.impl.cache;

public final class CacheConstants {
    //Env variables
    public static final String ENV_LIST_ACCOUNT_DISABLED = "osis.scality.vault.cache.listAccounts.disabled";
    public static final String ENV_LIST_ACCOUNT_MAX_CAPACITY = "osis.scality.vault.cache.listAccounts.maxCapacity";
    public static final String ENV_LIST_ACCOUNT_CACHE_TTL = "osis.scality.vault.cache.listAccounts.ttlInMS";
    public static final String ENV_ASSUME_ROLE_MAX_CAPACITY = "osis.scality.vault.cache.assumeRole.maxCapacity";
    public static final String ENV_ASSUME_ROLE_CACHE_TTL = "osis.scality.vault.cache.assumeRole.ttlInMS";
    public static final String ENV_ACCOUNT_ID_DISABLED = "osis.scality.vault.cache.accountID.disabled";
    public static final String ENV_ACCOUNT_ID_MAX_CAPACITY = "osis.scality.vault.cache.accountID.maxCapacity";
    public static final String ENV_ACCOUNT_ID_CACHE_TTL = "osis.scality.vault.cache.accountID.ttlInMS";



    // Defaults
    public static final int DEFAULT_CACHE_MAX_CAPACITY = 1000;
    public static final long DEFAULT_CACHE_TTL_IN_MS = 30000;
    public static final int DEFAULT_SCHEDULED_THREAD_POOL_SIZE = 1;

    //Constants
    public static final String NAME_LIST_ACCOUNTS_CACHE = "listAccounts";
    public static final String NAME_ASSUME_ROLE_CACHE = "assumeRole";
    public static final String NAME_ACCOUNT_ID_CACHE = "accountIDs";

    private CacheConstants(){}

}
