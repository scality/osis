package com.scality.osis.security.utils;

public final class ScalitySecurityTestUtils {

    public static final String TEST_ACCESS_KEY = "access_key";
    public static final String TEST_SECRET_KEY = "secret_key";

    public static final String TEST_CIPHER_SECRET_KEY = "dGhpc2lzYXJlYWxseWxvbmdhbmRzZXk=";
    public static final String TEST_INVALID_CIPHER_SECRET_KEY = "dGhpc2lzYXJlYWxseWxvbmdhbmRzZXk=hgJfad==";
    public static final String TEST_CIPHER_ID = "1";

    public static final String TEST_ADMIN_CREDS_FILE = "[{\"salt\":\"U2N6QzE0TU93Q1BveXdDNDd5ZzE=\",\"tag\":\"0wMtd7wvixOc8/x86qhE/A==\",\"value\":\"VOaE/u1wsUA3yleD2qCj6lS7JXs+l/QV8+XjqCDBgpslk1q3VPOKeQ==\"}]";
    public static final String TEST_ADMIN_CREDS_FILE_INVALID_TAG = "[{\"salt\":\"U2N6QzE0TU93Q1BveXdDNDd5ZzE=\",\"tag\":\"XXXXXwvixOc8/x86qhE/A==\",\"value\":\"VOaE/u1wsUA3yleD2qCj6lS7JXs+l/QV8+XjqCDBgpslk1q3VPOKeQ==\"}]";
    public static final String TEST_MASTER_KEY = "Ct+kvVF+k5QpI9HrCJcgD22nsoCRMj5l/kj3DT5OaschNhjUEe5mWamCXS55OjQ/,E7kpsyx1qmc5A5LNFY2kfbo6e6+BVDFHiT6UXArtlbJ8kl9/RN+71eZPl+WS8quG,";
    public static final String TEST_ADMIN_ACCESS_KEY = "RBCF8Y9XX60A0H61NDKX";
    public static final String TEST_ADMIN_SECRET_KEY = "fZiHFq1Sln0O9myRL5RCmhfVweQfzDG6H/jrqCk=";

    private ScalitySecurityTestUtils(){

    }
}
