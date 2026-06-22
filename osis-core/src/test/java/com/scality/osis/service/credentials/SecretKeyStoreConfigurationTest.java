/**
 * Copyright 2024 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.service.credentials;

import com.scality.osis.ScalityAppEnv;
import com.scality.osis.redis.service.IRedisRepository;
import com.scality.osis.security.crypto.model.SecretKeyRepoData;
import com.scality.osis.security.utils.CipherFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static com.scality.osis.utils.ScalityConstants.REDIS_SPRING_CACHE_TYPE;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.when;

class SecretKeyStoreConfigurationTest {

    @Mock
    private ScalityAppEnv appEnv;

    @Mock
    private IRedisRepository<SecretKeyRepoData> redisRepository;

    @Mock
    private CipherFactory cipherFactory;

    private SecretKeyStoreConfiguration configuration;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        configuration = new SecretKeyStoreConfiguration();
    }

    @Test
    void selectsRedisBackendWhenCacheTypeIsRedis() {
        when(appEnv.getSpringCacheType()).thenReturn(REDIS_SPRING_CACHE_TYPE);
        assertInstanceOf(RedisSecretKeyBackend.class,
                configuration.secretKeyBackend(appEnv, redisRepository));
    }

    @Test
    void selectsInMemoryBackendWhenCacheTypeIsNotRedis() {
        when(appEnv.getSpringCacheType()).thenReturn("simple");
        assertInstanceOf(InMemorySecretKeyBackend.class,
                configuration.secretKeyBackend(appEnv, redisRepository));
    }

    @Test
    void buildsEncryptedSecretKeyStore() {
        assertInstanceOf(EncryptedSecretKeyStore.class,
                configuration.secretKeyStore(new InMemorySecretKeyBackend(), cipherFactory));
    }
}
