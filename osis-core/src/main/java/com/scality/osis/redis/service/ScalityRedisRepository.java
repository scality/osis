/**
 *Copyright 2021 Scality, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.redis.service;

import com.scality.osis.utils.ScalityModelConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;

import static com.scality.osis.utils.ScalityConstants.DEFAULT_REDIS_HASH_KEY;

@Repository
public class ScalityRedisRepository<T> implements IRedisRepository<T> {

    @Autowired
    RedisTemplate<String, T> redisTemplate;

    @Value("${osis.scality.redis.credentials.hashKey}")
    String osisRedisHashKey = DEFAULT_REDIS_HASH_KEY;

    private HashOperations<String, String, T> hashOperations;

    @PostConstruct
    public void postInit() {
        hashOperations = redisTemplate.opsForHash();
    }

    @Override
    public void save(String key, T value) {
        hashOperations.put(ScalityModelConverter.toRedisHashName(osisRedisHashKey), key, value);
    }

    @Override
    public T get(String key) {
        return hashOperations.get(ScalityModelConverter.toRedisHashName(osisRedisHashKey), key);
    }

    @Override
    public void delete(String key) {
        hashOperations.delete(ScalityModelConverter.toRedisHashName(osisRedisHashKey), key);
    }

    @Override
    public Boolean hasKey(String key) {
        return hashOperations.hasKey(ScalityModelConverter.toRedisHashName(osisRedisHashKey), key);
    }

}
