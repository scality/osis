/**
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.security.crypto;

import com.scality.osis.security.crypto.model.AES256GCMInformation;
import com.scality.osis.security.crypto.model.EncryptedAdminCredentials;
import com.scality.osis.security.crypto.model.SecretKeyRepoData;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.Base64;

import static com.scality.osis.security.utils.SecurityConstants.DEFAULT_AES_GCM_256_KEY_LENGTH;
import static com.scality.osis.security.utils.SecurityConstants.DEFAULT_AES_GCM_NONCE_LENGTH;
import static com.scality.osis.security.utils.SecurityConstants.DEFAULT_AES_GCM_TAG_LENGTH;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.crypto.Cipher.DECRYPT_MODE;

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

    /**
     * Decrypts content previously encrypted by Vault
     * @param key  a master key string
     * @param adminCredentials  adminCredentials object with cipherText, salt, tag
     * @param info Information string used to decrypt the cipherText
     * @return original plaintext without padding
     */
    public String decryptHKDF(String key, EncryptedAdminCredentials adminCredentials, String info) throws Exception {
        String salt = adminCredentials.getSalt();
        String tag = adminCredentials.getTag();
        String cipherText = adminCredentials.getValue();

        //key derivation
        byte[] derivedKeyBytes = deriveKey(Base64.getDecoder().decode(salt),
                key.getBytes(StandardCharsets.UTF_8),
                info.getBytes(StandardCharsets.UTF_8));

        byte[] keyBytes = Arrays.copyOfRange(derivedKeyBytes, 0, DEFAULT_AES_GCM_256_KEY_LENGTH);
        byte[] ivBytes = Arrays.copyOfRange(derivedKeyBytes, DEFAULT_AES_GCM_256_KEY_LENGTH, (DEFAULT_AES_GCM_256_KEY_LENGTH + DEFAULT_AES_GCM_NONCE_LENGTH));

        SecretKey derivedKey = new SecretKeySpec(keyBytes, "AES");

        // decryption part
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmParameters = new GCMParameterSpec(DEFAULT_AES_GCM_TAG_LENGTH, ivBytes);

        cipher.init(DECRYPT_MODE, derivedKey, gcmParameters);

        cipher.update(Base64.getDecoder().decode(cipherText));
        return new String(cipher.doFinal(Base64.getDecoder().decode(tag)), UTF_8);
    }

    /**
     * Derives the key by Extracting and Expanding using HKDF
     * @param salt  salt bytes
     * @param key key bytes
     * @param info info bytes used to expand
     * @return symmetric key used in original encryption by Vault
     */
    private byte[] deriveKey(byte[] salt, byte[] key, byte[] info) {
        Hkdf hkdf = Hkdf.usingHash(Hkdf.Hash.SHA256);

        SecretKey secretKey = hkdf.extract(new SecretKeySpec(salt, Hkdf.Hash.SHA256.getAlgorithm()), key);

        return hkdf.expand(secretKey, info, (DEFAULT_AES_GCM_256_KEY_LENGTH + DEFAULT_AES_GCM_NONCE_LENGTH));

    }
}
