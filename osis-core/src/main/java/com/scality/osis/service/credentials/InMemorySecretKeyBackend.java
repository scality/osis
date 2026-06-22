/**
 * Copyright 2024 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.service.credentials;

import com.scality.osis.security.crypto.model.SecretKeyRepoData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Process-local {@link SecretKeyBackend} backed by a {@link ConcurrentHashMap}.
 *
 * <p>Used when Redis is not configured (i.e. {@code spring.cache.type} is not {@code redis}). Records
 * live only for the lifetime of the JVM.</p>
 */
public class InMemorySecretKeyBackend implements SecretKeyBackend {

    private final Map<String, SecretKeyRepoData> store = new ConcurrentHashMap<>();

    @Override
    public SecretKeyRepoData get(String repoKey) {
        return store.get(repoKey);
    }

    @Override
    public void save(String repoKey, SecretKeyRepoData value) {
        store.put(repoKey, value);
    }

    @Override
    public void delete(String repoKey) {
        store.remove(repoKey);
    }
}
