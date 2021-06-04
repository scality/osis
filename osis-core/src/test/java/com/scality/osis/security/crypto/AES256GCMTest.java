package com.scality.osis.security.crypto;

import com.scality.osis.security.crypto.model.SecretKeyRepoData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.AEADBadTagException;
import java.security.InvalidKeyException;

import static com.scality.osis.utils.ScalityTestUtils.TEST_ACCESS_KEY;
import static com.scality.osis.utils.ScalityTestUtils.TEST_CIPHER_SECRET_KEY;
import static com.scality.osis.utils.ScalityTestUtils.TEST_INVALID_CIPHER_SECRET_KEY;
import static com.scality.osis.utils.ScalityTestUtils.TEST_SECRET_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
