/**
 *Copyright 2021 Scality, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.service.impl;

import com.scality.osis.service.IRedisRepository;
import com.scality.osis.utils.ScalityModelConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import static com.scality.osis.utils.ScalityConstants.DEFAULT_REDIS_HASH_KEY;

@Repository
public class ScalityRedisRepository implements IRedisRepository {

    @Autowired
    StringRedisTemplate redisTemplate;

    @Value("${osis.scality.redis.credentials.hashKey}")
    String osisRedisHashKey = DEFAULT_REDIS_HASH_KEY;

    @Override
    public void save(String key, String value) {
        redisTemplate.opsForHash().put(ScalityModelConverter.toRedisHashName(osisRedisHashKey), key, value);
    }

    @Override
    public String get(String key) {
        return (String) redisTemplate.opsForHash().get(ScalityModelConverter.toRedisHashName(osisRedisHashKey), key);
    }

    @Override
    public void delete(String key) {
        redisTemplate.opsForHash().delete(ScalityModelConverter.toRedisHashName(osisRedisHashKey), key);
    }

    @Override
    public Boolean hasKey(String key) {
        return redisTemplate.opsForHash().hasKey(ScalityModelConverter.toRedisHashName(osisRedisHashKey), key);
    }

}
