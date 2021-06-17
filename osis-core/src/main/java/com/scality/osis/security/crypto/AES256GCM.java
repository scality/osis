/**
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.security.crypto;

import com.scality.osis.security.crypto.model.AES256GCMInformation;
import com.scality.osis.security.crypto.model.CipherInformation;
import com.scality.osis.security.crypto.model.SecretKeyRepoData;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Base64;

import static com.scality.osis.utils.ScalityConstants.DEFAULT_AES_GCM_NONCE_LENGTH;
import static com.scality.osis.utils.ScalityConstants.DEFAULT_AES_GCM_TAG_LENGTH;

public final class AES256GCM implements BaseCipher {
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Encrypt a plaintext with given key.
     *
     * @param plaintext      to encrypt (utf-8 encoding will be used)
     * @param secretKeyStr      to encrypt
     * @param associatedData optional, additional (public) data to verify on decryption with GCM auth tag
     * @return encrypted message
     * @throws Exception if anything goes wrong
     */
    @Override
    public SecretKeyRepoData encrypt(String plaintext, String secretKeyStr, String associatedData) throws Exception {

        byte[] nonce = new byte[DEFAULT_AES_GCM_NONCE_LENGTH]; //NEVER REUSE THIS NONCE WITH SAME KEY
        secureRandom.nextBytes(nonce);

        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(DEFAULT_AES_GCM_TAG_LENGTH, nonce);

        SecretKey secretKey = new SecretKeySpec(secretKeyStr.getBytes(StandardCharsets.UTF_8), "AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

        if (!StringUtils.isEmpty(associatedData)) {
            cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
        }

        byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        SecretKeyRepoData secretKeyRepoData = new SecretKeyRepoData();
        secretKeyRepoData.setEncryptedBytes(cipherText);
        secretKeyRepoData.setCipherInfo(AES256GCMInformation.builder()
                                        .nonce(nonce)
                                        .build());

        return secretKeyRepoData;
    }

    /**
     * Decrypts encrypted message (see {@link #encrypt(String, String, String)}).
     *
     * @param encryptedKeyRepoData  a cipher object along with nonce
     * @param secretKeyStr      used to decrypt
     * @param associatedData optional, additional (public) data to verify on decryption with GCM auth tag
     * @return original plaintext
     * @throws Exception if anything goes wrong
     */
    @Override
    public String decrypt(SecretKeyRepoData encryptedKeyRepoData, String secretKeyStr, String associatedData) throws Exception {
        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        byte[] cipherMessageBytes = encryptedKeyRepoData.getEncryptedBytes();
        byte[] nonce = ((AES256GCMInformation) encryptedKeyRepoData.getCipherInfo()).getNonce();

        AlgorithmParameterSpec gcmNonce = new GCMParameterSpec(DEFAULT_AES_GCM_TAG_LENGTH, nonce);

        SecretKey secretKey = new SecretKeySpec(secretKeyStr.getBytes(StandardCharsets.UTF_8), "AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmNonce);

        if (!StringUtils.isEmpty(associatedData)) {
            cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
        }

        byte[] plainText = cipher.doFinal(cipherMessageBytes);

        return new String(plainText, StandardCharsets.UTF_8);
    }
}
