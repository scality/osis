/**
 *Copyright 2021 Scality, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.service;

public interface IRedisRepository {
    void save(String key, String value);

    String get(String key);

    void delete(String key);

    Boolean hasKey(String key);
}
