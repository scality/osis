/**
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.vaultadmin.impl.cache;

public interface Cache<K,V> {
    V put(K key, V value);
    V remove(K key);
    V get(K key);
    void clear();
    long size();
}
