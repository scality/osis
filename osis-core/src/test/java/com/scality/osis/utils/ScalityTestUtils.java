package com.scality.osis.utils;

import com.scality.osis.model.OsisTenant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ScalityTestUtils {

    public static final String SAMPLE_TENANT_ID = "123123123123";
    public static final String SAMPLE_ID = "f3bbe501-29b0-4891-8602-a4e2f95101f8";
    public static final String SAMPLE_CD_TENANT_ID = "5db7b952-66b9-4164-bfe2-7bab25ac9011";
    public static final String SAMPLE_TENANT_NAME = "tenant name";
    public static final List<String> SAMPLE_CD_TENANT_IDS = new ArrayList<>(
            Arrays.asList(
                    "9b7e3259-aace-414c-bfd8-94daa0efefaf", "7b7e3259-aace-414c-bfd8-94daa0efefaf",
                    "5b7e3259-aace-414c-bfd8-94daa0efefaf", "6b7e3259-aace-414c-bfd8-94daa0efefaf",
                    "4b7e3259-aace-414c-bfd8-94daa0efefaf", "3b7e3259-aace-414c-bfd8-94daa0efefaf"));
    public static final String TEST_ROLE_ARN = "arn:aws:iam::123123123123:role/osis";
    public static final String TEST_SESSION_NAME_PREFIX = "ROLE_SESSION_";
    public static final String SAMPLE_ASSUME_ROLE_NAME = "osis";

    public static final Map<String, String> SAMPLE_CUSTOM_ATTRIBUTES = new HashMap<>();
    static {
        SAMPLE_CUSTOM_ATTRIBUTES.put("cd_tenant_id==9b7e3259-aace-414c-bfd8-94daa0efefaf", "");
        SAMPLE_CUSTOM_ATTRIBUTES.put("cd_tenant_id==7b7e3259-aace-414c-bfd8-94daa0efefaf", "");
        SAMPLE_CUSTOM_ATTRIBUTES.put("cd_tenant_id==5b7e3259-aace-414c-bfd8-94daa0efefaf", "");
        SAMPLE_CUSTOM_ATTRIBUTES.put("cd_tenant_id==6b7e3259-aace-414c-bfd8-94daa0efefaf", "");
        SAMPLE_CUSTOM_ATTRIBUTES.put("cd_tenant_id==4b7e3259-aace-414c-bfd8-94daa0efefaf", "");
        SAMPLE_CUSTOM_ATTRIBUTES.put("cd_tenant_id==3b7e3259-aace-414c-bfd8-94daa0efefaf", "");
    }

    public static final String SAMPLE_SCALITY_ACCOUNT_EMAIL = "tenant.name@osis.scality.com";
    public static final String SAMPLE_SCALITY_USER_EMAIL = "user.name@osis.scality.com";

    public static final String TEST_TENANT_ID = "tenantId";
    public static final String TEST_CD_TENANT_ID = "cdTenantId";
    public static final String TEST_STR = "value";
    public static final String NOT_IMPLEMENTED_EXCEPTION_ERR = "expected NotImplementedException";
    public static final String NULL_ERR = "Expected Value. Found Null";
    public static final String INVALID_URL_URL = "Invalid URL";
    public static final String TEST_USER_ID = "userId";
    public static final String TEST_CANONICAL_ID = "canonicalID";
    public static final String TEST_NAME = "name";
    public static final String TEST_BUCKET_NAME = "bucket_name";
    public static final String TEST_POLICY_ARN = "policy-arn";
    public static final String TEST_ACCESS_KEY = "access_key";
    public static final String TEST_ACCESS_KEY_2 = "access_key_2";
    public static final String TEST_SECRET_KEY = "secret_key";
    public static final String TEST_SESSION_TOKEN = "session_token";
    public static final String TEST_CONSOLE_URL = "https://example.console.ose.scality.com";
    public static final String TEST_S3_INTERFACE_URL = "https://localhost:8443";
    public static final String TEST_S3_CAPABILITIES_FILE_PATH = "s3capabilities.json";
    public static final String TEST_S3_URL = "http://localhost:8000";
    public static final String TEST_UTAPI_URL = "http://localhost:8100";
    public static final String PLATFORM_NAME = "Scality";
    public static final String PLATFORM_VERSION = "7.10";
    public static final String API_VERSION = "1.5.0-SNAPSHOT";
    public static final long SAMPLE_DURATION_SECONDS = 120L;
    public static final String ACTIVE_STR = "Active";
    public static final String NA_STR = "N/A";

    public static final String TEST_CIPHER_SECRET_KEY = "dGhpc2lzYXJlYWxseWxvbmdhbmRzZXk=";
    public static final String TEST_INVALID_CIPHER_SECRET_KEY = "dGhpc2lzYXJlYWxseWxvbmdhbmRzZXk=hgJfad==";
    public static final String TEST_CIPHER_ID = "1";

    public static final long TEST_BUCKET_TOTAL_NUMBER = 10000L;

    private ScalityTestUtils() {

    }

    public static OsisTenant createSampleOsisTenantObj() {
        final OsisTenant osisTenantReq = new OsisTenant();
        osisTenantReq.tenantId(SAMPLE_ID);
        osisTenantReq.name(SAMPLE_TENANT_NAME);
        osisTenantReq.cdTenantIds(SAMPLE_CD_TENANT_IDS);
        osisTenantReq.active(true);
        return osisTenantReq;
    }
}
