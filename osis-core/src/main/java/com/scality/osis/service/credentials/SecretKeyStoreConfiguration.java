/**
 * Copyright 2024 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.service.credentials;

import com.scality.osis.ScalityAppEnv;
import com.scality.osis.redis.service.IRedisRepository;
import com.scality.osis.security.crypto.model.SecretKeyRepoData;
import com.scality.osis.security.utils.CipherFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.scality.osis.utils.ScalityConstants.REDIS_SPRING_CACHE_TYPE;

/**
 * Wires the {@link SecretKeyStore}: picks the {@link SecretKeyBackend} from
 * {@code spring.cache.type} (Redis when {@code redis}, otherwise process-local) and wraps it in an
 * {@link EncryptedSecretKeyStore}.
 */
@Configuration
public class SecretKeyStoreConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(SecretKeyStoreConfiguration.class);

    @Bean
    public SecretKeyBackend secretKeyBackend(ScalityAppEnv appEnv,
                                             IRedisRepository<SecretKeyRepoData> redisRepository) {
        if (REDIS_SPRING_CACHE_TYPE.equalsIgnoreCase(appEnv.getSpringCacheType())) {
            logger.info("[SecretKeyStore] Using Redis backend for secret-key storage");
            return new RedisSecretKeyBackend(redisRepository);
        }
        logger.info("[SecretKeyStore] Using in-memory backend for secret-key storage");
        return new InMemorySecretKeyBackend();
    }

    @Bean
    public SecretKeyStore secretKeyStore(SecretKeyBackend secretKeyBackend, CipherFactory cipherFactory) {
        return new EncryptedSecretKeyStore(secretKeyBackend, cipherFactory);
    }
}
