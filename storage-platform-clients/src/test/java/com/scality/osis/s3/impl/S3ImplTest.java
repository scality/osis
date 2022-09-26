package com.scality.osis.s3.impl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.securitytoken.model.Credentials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;

import java.io.IOException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class S3ImplTest {

    public S3Impl s3Impl;
    public String s3Endpoint;

    public static final String TEST_ACCESS_KEY = "access_key";
    public static final String TEST_SECRET_KEY = "secret_key";
    public static final String TEST_SESSION_TOKEN = "session_token";
    public static final String TEST_REGION = "us-east-1";

    @BeforeEach
    public void init() throws IOException {
        String env = System.getProperty("env", "");
        if (!StringUtils.isBlank(env)) {
            env = "." + env;
        }
        final Properties properties = new Properties();
        properties.load(S3ImplTest.class.getResourceAsStream("/storage-platform-clients.properties" + env));

        s3Endpoint = properties.getProperty("s3.endpoint");
        s3Impl = new S3Impl(s3Endpoint);
    }

    @Test
    public void testS3ImplInvalidEndpoint() {
        assertThrows(IllegalArgumentException.class,
                () -> new S3Impl("invalid_s3_endpoint"),
                "S3Impl constructor should throw IllegalArgumentException for null endpoint");
    }

    @Test
    public void testGetS3Client() {
        final Credentials credentials = new Credentials();
        credentials.setAccessKeyId(TEST_ACCESS_KEY);
        credentials.setSecretAccessKey(TEST_SECRET_KEY);

        final AmazonS3 s3Client = s3Impl.getS3Client(credentials, TEST_REGION);
        assertNotNull(s3Client);
    }

    @Test
    public void testGetS3ClientWithSession() {
        final Credentials credentials = new Credentials();
        credentials.setAccessKeyId(TEST_ACCESS_KEY);
        credentials.setSecretAccessKey(TEST_SECRET_KEY);
        credentials.setSessionToken(TEST_SESSION_TOKEN);

        final AmazonS3 s3Client = s3Impl.getS3Client(credentials, TEST_REGION);
        assertNotNull(s3Client);
    }
}
