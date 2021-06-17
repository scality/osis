/**
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.security.crypto;

import com.scality.osis.security.crypto.model.SecretKeyRepoData;

import javax.crypto.SecretKey;

public interface BaseCipher {
    /**
     * Encrypt a plaintext with given key.
     *
     * @param plaintext      to encrypt (utf-8 encoding will be used)
     * @param secretKeyStr      to encrypt
     * @param associatedData optional, additional (public) data to verify on decryption with GCM auth tag
     * @return encrypted message
     * @throws Exception if anything goes wrong
     */
    SecretKeyRepoData encrypt(String plaintext, String secretKeyStr, String associatedData) throws Exception;

    /**
     * Decrypts encrypted message (see {@link #encrypt(String, SecretKey, String)}).
     *
     * @param encryptedKeyRepoData  a cipher object along with associated information
     * @param secretKeyStr      used to decrypt
     * @param associatedData optional, additional (public) data to verify on decryption with GCM auth tag
     * @return original plaintext
     * @throws Exception if anything goes wrong
     */
    String decrypt(SecretKeyRepoData encryptedKeyRepoData, String secretKeyStr, String associatedData) throws Exception;
}
