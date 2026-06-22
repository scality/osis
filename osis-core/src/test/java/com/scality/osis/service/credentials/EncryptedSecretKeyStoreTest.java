/**
 * Copyright 2024 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.service.credentials;

import com.scality.osis.security.crypto.CryptoEnv;
import com.scality.osis.security.crypto.model.SecretKeyRepoData;
import com.scality.osis.security.utils.CipherFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.scality.osis.security.utils.SecurityConstants.NAME_AES_256_GCM_CIPHER;
import static com.scality.osis.utils.ScalityTestUtils.TEST_CIPHER_ID;
import static com.scality.osis.utils.ScalityTestUtils.TEST_CIPHER_SECRET_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Exercises {@link EncryptedSecretKeyStore} against the real {@link CipherFactory}/AES256GCM cipher
 * and the {@link InMemorySecretKeyBackend}, so encryption, decryption, and on-decrypt-failure
 * eviction are all genuinely run rather than mocked.
 */
class EncryptedSecretKeyStoreTest {

    private static final String REPO_KEY = "user-1__AKIAEXAMPLE";
    private static final String SECRET = "s3cr3t-access-key-value";

    private InMemorySecretKeyBackend backend;
    private SecretKeyStore secretKeyStore;

    @BeforeEach
    void setUp() {
        backend = new InMemorySecretKeyBackend();

        final CryptoEnv.CipherKey cipherKey = new CryptoEnv.CipherKey();
        cipherKey.setId(TEST_CIPHER_ID);
        cipherKey.setCipher(NAME_AES_256_GCM_CIPHER);
        cipherKey.setSecretKey(TEST_CIPHER_SECRET_KEY);

        final CryptoEnv cryptoEnv = new CryptoEnv();
        cryptoEnv.setKeys(List.of(cipherKey));

        final CipherFactory cipherFactory = new CipherFactory();
        ReflectionTestUtils.setField(cipherFactory, "cryptoEnv", cryptoEnv);

        secretKeyStore = new EncryptedSecretKeyStore(backend, cipherFactory);
    }

    @Test
    void testStoreThenRetrieveReturnsOriginalSecret() throws Exception {
        secretKeyStore.store(REPO_KEY, SECRET);

        // The persisted record is encrypted, not the plaintext, and is stamped with the cipher id/name.
        final SecretKeyRepoData stored = backend.get(REPO_KEY);
        assertEquals(TEST_CIPHER_ID, stored.getKeyID());
        assertEquals(NAME_AES_256_GCM_CIPHER, stored.getCipherInfo().getCipherName());
        // AES-GCM ciphertext carries a 16-byte auth tag, so the stored bytes can't be the raw plaintext.
        assertNotEquals(SECRET.getBytes(StandardCharsets.UTF_8).length, stored.getEncryptedBytes().length);

        assertEquals(SECRET, secretKeyStore.retrieve(REPO_KEY));
    }

    @Test
    void testRetrieveMissingKeyReturnsNull() throws Exception {
        assertNull(secretKeyStore.retrieve("absent__key"));
    }

    @Test
    void testRetrieveEvictsAndReturnsNullWhenDecryptionFails() throws Exception {
        secretKeyStore.store(REPO_KEY, SECRET);

        // Corrupt the stored ciphertext so AES-GCM authentication fails on decrypt.
        final SecretKeyRepoData corrupted = backend.get(REPO_KEY);
        corrupted.getEncryptedBytes()[0] ^= (byte) 0xFF;
        backend.save(REPO_KEY, corrupted);

        assertNull(secretKeyStore.retrieve(REPO_KEY));
        // The undecryptable record must be evicted.
        assertNull(backend.get(REPO_KEY));
    }

    @Test
    void testRetrieveUsesRepoKeyAsAssociatedData() throws Exception {
        secretKeyStore.store(REPO_KEY, SECRET);

        // Re-home the same encrypted record under a different repoKey: the AAD no longer matches,
        // so decryption fails, the entry is evicted, and null is returned.
        final SecretKeyRepoData stored = backend.get(REPO_KEY);
        final String otherKey = "user-2__AKIAOTHER";
        backend.save(otherKey, stored);

        assertNull(secretKeyStore.retrieve(otherKey));
        assertNull(backend.get(otherKey));
    }

    @Test
    void testDeleteRemovesStoredSecret() throws Exception {
        secretKeyStore.store(REPO_KEY, SECRET);
        assertNotNull(backend.get(REPO_KEY));

        secretKeyStore.delete(REPO_KEY);

        assertNull(backend.get(REPO_KEY));
        assertNull(secretKeyStore.retrieve(REPO_KEY));
    }
}
