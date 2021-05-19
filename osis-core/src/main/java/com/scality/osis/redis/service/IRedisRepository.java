/**
 *Copyright 2021 Scality, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.redis.service;

public interface IRedisRepository<T> {
    void save(String key, T value);

    T get(String key);

    void delete(String key);

    Boolean hasKey(String key);
}
