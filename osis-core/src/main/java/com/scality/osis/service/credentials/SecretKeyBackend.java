/**
 * Copyright 2024 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.service.credentials;

import com.scality.osis.security.crypto.model.SecretKeyRepoData;

/**
 * Raw key/value backing store for already-encrypted secret-key records.
 *
 * <p>This is the storage seam behind {@link SecretKeyStore}: it moves opaque
 * {@link SecretKeyRepoData} blobs in and out by key and knows nothing about encryption. The two
 * adapters are {@link RedisSecretKeyBackend} (Redis Sentinel) and {@link InMemorySecretKeyBackend}
 * (process-local map); the active one is chosen in {@link SecretKeyStoreConfiguration}.</p>
 *
 * <p>Absence is signalled by {@link #get} returning {@code null}, and {@link #delete} is idempotent,
 * so there is no separate existence check.</p>
 */
public interface SecretKeyBackend {

    /**
     * @param repoKey the storage key
     * @return the stored record, or {@code null} if absent
     */
    SecretKeyRepoData get(String repoKey);

    /**
     * Store (or overwrite) the record for {@code repoKey}.
     *
     * @param repoKey the storage key
     * @param value   the encrypted record to persist
     */
    void save(String repoKey, SecretKeyRepoData value);

    /**
     * Remove the record for {@code repoKey}. A no-op if the key is absent.
     *
     * @param repoKey the storage key
     */
    void delete(String repoKey);
}
