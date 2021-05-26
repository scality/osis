/**
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.utils;

import com.scality.osis.security.crypto.AES256GCM;
import com.scality.osis.security.crypto.BaseCipher;
import com.scality.osis.security.crypto.CryptoEnv;
import com.scality.osis.vaultadmin.impl.VaultServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Comparator;

import static com.scality.osis.utils.ScalityConstants.NAME_AES_256_GCM_CIPHER;

/**
 * The factory class for all Ciphers.
 */
@Component
public class CipherFactory {

    @Autowired
    private CryptoEnv cryptoEnv;

    private BaseCipher baseCipher;

    public CipherFactory(){}

    private CryptoEnv.CipherKey getLatestCipherKey() {
        return cryptoEnv.getKeys()
                .stream()
                .max(Comparator.comparing(cipherKey -> Integer.parseInt(cipherKey.getId())))
                .get();
    }

    private CryptoEnv.CipherKey getCipherKeyByCipherID(String cipherID) {
        return cryptoEnv.getKeys()
                .stream()
                .filter(key -> cipherID.equals(key.getId()))
                .findAny()
                .get();
    }

    public String getLatestSecretCipherKey(){
        CryptoEnv.CipherKey maxCipherKey = getLatestCipherKey();
        if(maxCipherKey != null){
            return maxCipherKey.getSecretKey();
        }
        return null;
    }

    public String getSecretCipherKeyByID(String cipherID){
        CryptoEnv.CipherKey cipherKey = getCipherKeyByCipherID(cipherID);
        if(cipherKey != null){
            return cipherKey.getSecretKey();
        }
        return null;
    }

    public String getLatestCipherID(){
        CryptoEnv.CipherKey maxCipherKey = getLatestCipherKey();
        if(maxCipherKey != null){
            return maxCipherKey.getId();
        }
        return null;
    }

    public String getLatestCipherName(){
        CryptoEnv.CipherKey maxCipherKey = getLatestCipherKey();
        if(maxCipherKey != null){
            return maxCipherKey.getCipher();
        }
        return null;
    }

    /**
     * Get Cipher class from environment variables.
     *
     * @return the cache object
     */
    public BaseCipher getCipher(){
        return getCipherByName(getLatestCipherName());
    }

    /**
     * Get Cipher class from environment variables.
     *
     * @return the cache object
     */
    public BaseCipher getCipherByID(String cipherID){

        CryptoEnv.CipherKey cipherKey = getCipherKeyByCipherID(cipherID);

        String cipherName = (cipherKey != null) ?
                cipherKey.getCipher()
                : null;

        return getCipherByName(cipherName);
    }

    /**
     * Get Cipher class using cache name.
     *
     * @param cipherName the cipher name
     * @return the cache object
     */
    public BaseCipher getCipherByName(String cipherName){
        switch(cipherName){
            case NAME_AES_256_GCM_CIPHER :
                if((baseCipher == null) || !(baseCipher instanceof AES256GCM)) {
                    baseCipher = new AES256GCM();
                }

                return baseCipher;
            default : throw new VaultServiceException(HttpStatus.NOT_FOUND, "Unknown Cipher name");
        }
    }

}
