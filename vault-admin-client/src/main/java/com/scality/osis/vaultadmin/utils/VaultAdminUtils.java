package com.scality.osis.vaultadmin.utils;

import com.amazonaws.util.StringUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.scality.osis.security.crypto.AES256GCM;
import com.scality.osis.security.crypto.model.EncryptedAdminCredentials;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public class VaultAdminUtils {
    public static String getVaultSKEncryptedAdminFile(String accessKey, String adminFilePath, String masterKeyFilePath) {
        if(StringUtils.isNullOrEmpty(adminFilePath) || StringUtils.isNullOrEmpty(masterKeyFilePath)){
            return null;
        }
        try {
            String adminFileContent = new String ( Files.readAllBytes( Paths.get(adminFilePath)), UTF_8);
            String masterKeyFile = new String ( Files.readAllBytes( Paths.get(masterKeyFilePath)), UTF_8);

            final Type listType = new TypeToken<List<EncryptedAdminCredentials>>() {}.getType();
            final List<EncryptedAdminCredentials> encryptedAdminCredentialsList = new Gson().fromJson(adminFileContent, listType);

            for(EncryptedAdminCredentials adminCredentials : encryptedAdminCredentialsList) {
                AES256GCM aes256GCM = new AES256GCM();
                String sk = aes256GCM.decryptHKDF(masterKeyFile, adminCredentials, accessKey);

                // Encrypt the decrypted value to verify if correct SK
                if(adminCredentials.equals(aes256GCM.encryptHKDF(masterKeyFile, adminCredentials.getSalt(), sk, accessKey))){
                    // return when correct SK if found
                    return sk;
                }
            }
            return null;
        }
        catch (Exception e) {
            return null;
        }
    }
}
