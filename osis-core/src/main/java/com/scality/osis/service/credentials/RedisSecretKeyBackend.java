/**
 * Copyright 2024 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.service.credentials;

import com.scality.osis.redis.service.IRedisRepository;
import com.scality.osis.security.crypto.model.SecretKeyRepoData;

/**
 * {@link SecretKeyBackend} backed by Redis Sentinel via {@link IRedisRepository}.
 *
 * <p>Records land in the {@code osis:s3credentials} hash owned by the underlying repository.</p>
 */
public class RedisSecretKeyBackend implements SecretKeyBackend {

    private final IRedisRepository<SecretKeyRepoData> redisRepository;

    public RedisSecretKeyBackend(IRedisRepository<SecretKeyRepoData> redisRepository) {
        this.redisRepository = redisRepository;
    }

    @Override
    public SecretKeyRepoData get(String repoKey) {
        return redisRepository.get(repoKey);
    }

    @Override
    public void save(String repoKey, SecretKeyRepoData value) {
        redisRepository.save(repoKey, value);
    }

    @Override
    public void delete(String repoKey) {
        redisRepository.delete(repoKey);
    }
}
