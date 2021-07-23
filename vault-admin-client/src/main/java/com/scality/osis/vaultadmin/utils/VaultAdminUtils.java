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

            return new AES256GCM().decryptHKDF(masterKeyFile, encryptedAdminCredentialsList.get(0), accessKey);
        }
        catch (Exception e) {
            return null;
        }
    }
}
