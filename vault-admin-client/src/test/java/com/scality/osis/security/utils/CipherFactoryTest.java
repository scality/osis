package com.scality.osis.security.utils;

import com.scality.osis.security.crypto.AES256GCM;
import com.scality.osis.security.crypto.BaseCipher;
import com.scality.osis.security.crypto.CryptoEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static com.scality.osis.security.utils.SecurityConstants.NAME_AES_256_GCM_CIPHER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class CipherFactoryTest {

    @Mock
    private CryptoEnv cryptoEnvMock;

    private CipherFactory cipherFactoryUnderTest;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks( this );
        cipherFactoryUnderTest = new CipherFactory();
        initMocks();

        ReflectionTestUtils.setField(cipherFactoryUnderTest, "cryptoEnv", cryptoEnvMock);
    }

    private void initMocks() {
        final CryptoEnv.CipherKey cipherKey = new CryptoEnv.CipherKey();
        cipherKey.setId(ScalitySecurityTestUtils.TEST_CIPHER_ID);
        cipherKey.setCipher(NAME_AES_256_GCM_CIPHER);
        cipherKey.setSecretKey(ScalitySecurityTestUtils.TEST_CIPHER_SECRET_KEY);
        final List<CryptoEnv.CipherKey> keys = new ArrayList<>();
        keys.add(cipherKey);

        when(cryptoEnvMock.getKeys()).thenReturn(keys);
    }

    @Test
    public void testGetLatestSecretCipherKey() {
        // Setup

        // Run the test
        final String result = cipherFactoryUnderTest.getLatestSecretCipherKey();

        // Verify the results
        assertThat(result).isEqualTo(ScalitySecurityTestUtils.TEST_CIPHER_SECRET_KEY);
    }

    @Test
    public void testGetSecretCipherKeyByID() {
        // Setup

        // Run the test
        final String result = cipherFactoryUnderTest.getSecretCipherKeyByID(ScalitySecurityTestUtils.TEST_CIPHER_ID);

        // Verify the results
        assertThat(result).isEqualTo(ScalitySecurityTestUtils.TEST_CIPHER_SECRET_KEY);
    }

    @Test
    public void testGetLatestCipherID() {
        // Setup

        // Run the test
        final String result = cipherFactoryUnderTest.getLatestCipherID();

        // Verify the results
        assertThat(result).isEqualTo(ScalitySecurityTestUtils.TEST_CIPHER_ID);
    }

    @Test
    public void testGetLatestCipherName() {
        // Setup

        // Run the test
        final String result = cipherFactoryUnderTest.getLatestCipherName();

        // Verify the results
        assertThat(result).isEqualTo(NAME_AES_256_GCM_CIPHER);
    }

    @Test
    public void testGetCipher() {
        // Setup

        // Run the test
        final BaseCipher result = cipherFactoryUnderTest.getCipher();

        // Verify the results
        assertThat(result).isInstanceOf(AES256GCM.class);
    }

    @Test
    public void testGetCipherByID() {
        // Setup

        // Run the test
        final BaseCipher result = cipherFactoryUnderTest.getCipherByID(ScalitySecurityTestUtils.TEST_CIPHER_ID);

        // Verify the results
        assertThat(result).isInstanceOf(AES256GCM.class);
    }

    @Test
    public void testGetCipherByName() {
        // Setup

        // Run the test
        final BaseCipher result = cipherFactoryUnderTest.getCipherByName(NAME_AES_256_GCM_CIPHER);

        // Verify the results
        assertThat(result).isInstanceOf(AES256GCM.class);
    }
}
