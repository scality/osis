/**
 * Copyright 2024 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.service.credentials;

import com.scality.osis.security.crypto.model.SecretKeyRepoData;
import com.scality.osis.security.utils.CipherFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link SecretKeyStore} that encrypts with {@link CipherFactory} (AES-GCM, {@code repoKey} as
 * associated data) and persists through a {@link SecretKeyBackend}.
 *
 * <p>Each stored record is stamped with the id and name of the cipher key that encrypted it, so a
 * later retrieve can pick the matching key even across key rotation. If decryption fails the record
 * is treated as unrecoverable: it is evicted and {@code null} is returned.</p>
 */
public class EncryptedSecretKeyStore implements SecretKeyStore {

    private static final Logger logger = LoggerFactory.getLogger(EncryptedSecretKeyStore.class);

    private final SecretKeyBackend backend;
    private final CipherFactory cipherFactory;

    public EncryptedSecretKeyStore(SecretKeyBackend backend, CipherFactory cipherFactory) {
        this.backend = backend;
        this.cipherFactory = cipherFactory;
    }

    @Override
    public void store(String repoKey, String secret) throws Exception {
        // Using `repoKey` for Associated Data during encryption
        logger.debug("Store Secret Key. Key:{}", repoKey);
        SecretKeyRepoData encryptedRepoData = cipherFactory.getCipher().encrypt(secret,
                cipherFactory.getLatestSecretCipherKey(),
                repoKey);

        // Stamp the record with the encrypting key's id/name so retrieve can pick the matching key.
        encryptedRepoData.setKeyID(cipherFactory.getLatestCipherID());
        encryptedRepoData.getCipherInfo().setCipherName(cipherFactory.getLatestCipherName());

        backend.save(repoKey, encryptedRepoData);
        logger.debug("Store Secret Key successful. Key:{}", repoKey);
    }

    @Override
    public String retrieve(String repoKey) throws Exception {
        logger.debug("Retrieve Secret Key. Key:{}", repoKey);
        SecretKeyRepoData repoVal = backend.get(repoKey);

        String secretKey = null;

        if (repoVal != null) {
            try {
                // Using `repoKey` for Associated Data during decryption
                secretKey = cipherFactory.getCipherByID(repoVal.getKeyID())
                        .decrypt(repoVal,
                                cipherFactory.getSecretCipherKeyByID(repoVal.getKeyID()),
                                repoKey);

                logger.debug("Retrieve Secret Key successful. Key:{}", repoKey);
            } catch (Exception e) {
                logger.error("Error: Unable to decrypt secret key data for key: {}. Error details: {}", repoKey, e.getMessage());
                logger.debug("Full stack trace:", e);
                delete(repoKey);
            }
        }
        return secretKey;
    }

    @Override
    public void delete(String repoKey) throws Exception {
        logger.debug("Delete Secret Key. Key:{}", repoKey);
        backend.delete(repoKey);
        logger.debug("Delete Secret Key successful. Key:{}", repoKey);
    }
}
