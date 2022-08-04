package com.scality.osis.security.crypto;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.scality.osis.security.crypto.model.EncryptedAdminCredentials;
import com.scality.osis.security.crypto.model.SecretKeyRepoData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.AEADBadTagException;
import java.lang.reflect.Type;
import java.security.InvalidKeyException;
import java.util.List;

import static com.scality.osis.security.utils.ScalitySecurityTestUtils.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AES256GCMTest {

    private AES256GCM aes256GCMUnderTest;

    @BeforeEach
    public void setUp() {
        aes256GCMUnderTest = new AES256GCM();
    }

    @Test
    public void testEncryptDecrypt() throws Exception {
        //create new random key
        final String key = TEST_CIPHER_SECRET_KEY;
        

        final SecretKeyRepoData encryptedRepoData = aes256GCMUnderTest.encrypt(TEST_SECRET_KEY, key, TEST_ACCESS_KEY);
        assertNotNull(encryptedRepoData);

        final String decrypted = aes256GCMUnderTest.decrypt(encryptedRepoData, key, TEST_ACCESS_KEY);
        assertEquals(TEST_SECRET_KEY, decrypted);
    }

    @Test
    public void testEncryptThrowsException() {
        // Setup
        //create new random key of size 40
        final String key = TEST_INVALID_CIPHER_SECRET_KEY;


        // Run the test
        assertThrows(InvalidKeyException.class, () -> aes256GCMUnderTest.encrypt(TEST_SECRET_KEY, key, TEST_ACCESS_KEY));
    }

    @Test
    public void testDecryptInvalidAssociatedDataThrowsException() throws Exception {
        // Setup
        //create new random key
        final String key = TEST_CIPHER_SECRET_KEY;
        

        final SecretKeyRepoData encryptedRepoData = aes256GCMUnderTest.encrypt(TEST_SECRET_KEY, key, TEST_ACCESS_KEY);
        // Run the test
        assertThrows(AEADBadTagException.class, () -> aes256GCMUnderTest.decrypt(encryptedRepoData, key, "InvalidData"));
    }

    @Test
    public void testDecryptHKDF() throws Exception {
        // Setup
        //create EncryptedAdminCredentials from json file
        final Type listType = new TypeToken<List<EncryptedAdminCredentials>>() {}.getType();
        final List<EncryptedAdminCredentials> encryptedAdminCredentialsList = new Gson().fromJson(TEST_ADMIN_CREDS_FILE, listType);


        // Run the test
        final String decryptedAdminSecretKey = aes256GCMUnderTest.decryptHKDF(TEST_MASTER_KEY, encryptedAdminCredentialsList.get(0),TEST_ADMIN_ACCESS_KEY );

        assertEquals(TEST_ADMIN_SECRET_KEY, decryptedAdminSecretKey);
    }

    @Test
    public void testDecryptHKDFThrowsException() throws Exception {
        // Setup
        //create EncryptedAdminCredentials from json file
        final Type listType = new TypeToken<List<EncryptedAdminCredentials>>() {}.getType();
        final List<EncryptedAdminCredentials> encryptedAdminCredentialsList = new Gson().fromJson(TEST_ADMIN_CREDS_FILE_INVALID_TAG, listType);


        // Run the test
        assertThrows(IllegalArgumentException.class, () -> aes256GCMUnderTest.decryptHKDF(TEST_MASTER_KEY, encryptedAdminCredentialsList.get(0),TEST_ADMIN_ACCESS_KEY ));
    }

    @Test
    public void testDecryptHKDFInvalidInfoThrowsException() throws Exception {
        // Setup
        //create EncryptedAdminCredentials from json file
        final Type listType = new TypeToken<List<EncryptedAdminCredentials>>() {}.getType();
        final List<EncryptedAdminCredentials> encryptedAdminCredentialsList = new Gson().fromJson(TEST_ADMIN_CREDS_FILE, listType);


        // Run the test
        assertThrows(AEADBadTagException.class, () -> aes256GCMUnderTest.decryptHKDF(TEST_MASTER_KEY, encryptedAdminCredentialsList.get(0),"InvalidData" ));
    }

    @Test
    public void testEncryptHKDF() throws Exception {
        // Setup
        //create EncryptedAdminCredentials from json file
        final Type listType = new TypeToken<List<EncryptedAdminCredentials>>() {}.getType();
        final List<EncryptedAdminCredentials> encryptedAdminCredentialsList = new Gson().fromJson(TEST_ADMIN_CREDS_FILE, listType);


        // Run the test
        final EncryptedAdminCredentials result = aes256GCMUnderTest.encryptHKDF(TEST_MASTER_KEY, encryptedAdminCredentialsList.get(0).getSalt(),
                TEST_ADMIN_SECRET_KEY, TEST_ADMIN_ACCESS_KEY );

        assertEquals(encryptedAdminCredentialsList.get(0).getValue(), result.getValue());
        assertEquals(encryptedAdminCredentialsList.get(0).getSalt(), result.getSalt());
        assertEquals(encryptedAdminCredentialsList.get(0).getTag(), result.getTag());
    }
}
