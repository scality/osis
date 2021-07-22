package com.scality.osis.security.crypto;

import com.scality.osis.security.crypto.model.SecretKeyRepoData;
import com.scality.osis.security.utils.ScalitySecurityTestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.AEADBadTagException;
import java.security.InvalidKeyException;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AES256GCMTest {

    private AES256GCM aes256GCMUnderTest;

    @BeforeEach
    public void setUp() {
        aes256GCMUnderTest = new AES256GCM();
    }

    @Test
    public void testEncryptDecrypt() throws Exception {
        //create new random key
        final String key = ScalitySecurityTestUtils.TEST_CIPHER_SECRET_KEY;
        

        final SecretKeyRepoData encryptedRepoData = aes256GCMUnderTest.encrypt(ScalitySecurityTestUtils.TEST_SECRET_KEY, key, ScalitySecurityTestUtils.TEST_ACCESS_KEY);
        assertNotNull(encryptedRepoData);

        final String decrypted = aes256GCMUnderTest.decrypt(encryptedRepoData, key, ScalitySecurityTestUtils.TEST_ACCESS_KEY);
        Assertions.assertEquals(ScalitySecurityTestUtils.TEST_SECRET_KEY, decrypted);
    }

    @Test
    public void testEncryptThrowsException() {
        // Setup
        //create new random key of size 40
        final String key = ScalitySecurityTestUtils.TEST_INVALID_CIPHER_SECRET_KEY;


        // Run the test
        assertThrows(InvalidKeyException.class, () -> aes256GCMUnderTest.encrypt(ScalitySecurityTestUtils.TEST_SECRET_KEY, key, ScalitySecurityTestUtils.TEST_ACCESS_KEY));
    }

    @Test
    public void testDecryptInvalidAssociatedDataThrowsException() throws Exception {
        // Setup
        //create new random key
        final String key = ScalitySecurityTestUtils.TEST_CIPHER_SECRET_KEY;
        

        final SecretKeyRepoData encryptedRepoData = aes256GCMUnderTest.encrypt(ScalitySecurityTestUtils.TEST_SECRET_KEY, key, ScalitySecurityTestUtils.TEST_ACCESS_KEY);
        // Run the test
        assertThrows(AEADBadTagException.class, () -> aes256GCMUnderTest.decrypt(encryptedRepoData, key, "InvalidData"));
    }
}
