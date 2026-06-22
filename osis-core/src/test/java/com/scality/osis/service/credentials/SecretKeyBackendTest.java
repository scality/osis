/**
 * Copyright 2024 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.service.credentials;

import com.scality.osis.redis.service.IRedisRepository;
import com.scality.osis.security.crypto.model.SecretKeyRepoData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecretKeyBackendTest {

    private static final String REPO_KEY = "user-1__AKIAEXAMPLE";

    @Mock
    private IRedisRepository<SecretKeyRepoData> redisRepository;

    private RedisSecretKeyBackend redisBackend;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        redisBackend = new RedisSecretKeyBackend(redisRepository);
    }

    @Test
    void testRedisBackendDelegatesGetSaveDelete() {
        final SecretKeyRepoData value = new SecretKeyRepoData();
        when(redisRepository.get(REPO_KEY)).thenReturn(value);

        assertSame(value, redisBackend.get(REPO_KEY));

        redisBackend.save(REPO_KEY, value);
        verify(redisRepository).save(REPO_KEY, value);

        // Delete is unconditional: Redis HDEL on a missing field is already a no-op.
        redisBackend.delete(REPO_KEY);
        verify(redisRepository).delete(REPO_KEY);
    }

    @Test
    void testRedisBackendGetReturnsNullWhenAbsent() {
        when(redisRepository.get(REPO_KEY)).thenReturn(null);
        assertNull(redisBackend.get(REPO_KEY));
    }

    @Test
    void testInMemoryBackendStoresAndDeletes() {
        final InMemorySecretKeyBackend backend = new InMemorySecretKeyBackend();
        final SecretKeyRepoData value = new SecretKeyRepoData();

        assertNull(backend.get(REPO_KEY));

        backend.save(REPO_KEY, value);
        assertSame(value, backend.get(REPO_KEY));

        backend.delete(REPO_KEY);
        assertNull(backend.get(REPO_KEY));
    }
}
