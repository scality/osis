/**
 * Copyright 2024 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.service.credentials;

/**
 * Stores S3 secret access keys at rest, encrypted.
 *
 * <p>Callers hand over a plaintext secret together with a {@code repoKey} (the storage identity of
 * the credential, e.g. {@code <userId>__<accessKeyId>}). The store owns the encryption invariant:
 * the secret is encrypted before it leaves this boundary and decrypted only when retrieved, with
 * {@code repoKey} bound in as the AES-GCM associated data. Whether the encrypted bytes live in Redis
 * or in process memory is hidden behind a {@link SecretKeyBackend}.</p>
 */
public interface SecretKeyStore {

    /**
     * Encrypt {@code secret} (with {@code repoKey} as associated data) and persist it under
     * {@code repoKey}.
     *
     * @param repoKey the storage identity of the credential, also used as encryption associated data
     * @param secret  the plaintext secret access key to store
     * @throws Exception if encryption or the backend write fails
     */
    void store(String repoKey, String secret) throws Exception;

    /**
     * Fetch and decrypt the secret stored under {@code repoKey}.
     *
     * <p>If no entry exists the result is {@code null}. If an entry exists but cannot be decrypted
     * (e.g. cipher-key rotation made it unrecoverable), the entry is evicted and {@code null} is
     * returned.</p>
     *
     * @param repoKey the storage identity of the credential
     * @return the plaintext secret, or {@code null} when absent or undecryptable
     * @throws Exception if the backend read or an eviction triggered by a decrypt failure fails
     */
    String retrieve(String repoKey) throws Exception;

    /**
     * Remove any secret stored under {@code repoKey}.
     *
     * @param repoKey the storage identity of the credential
     * @throws Exception if the backend delete fails
     */
    void delete(String repoKey) throws Exception;
}
