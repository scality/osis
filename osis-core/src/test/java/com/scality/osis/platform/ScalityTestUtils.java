package com.scality.osis.platform;


import com.scality.osis.model.OsisTenant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ScalityTestUtils {

    public static final String SAMPLE_TENANT_ID = "12313265465789";
    public static final String SAMPLE_ID = "bfc0d4a51e06481cbc917e9d96e52d81";
    public static final String SAMPLE_TENANT_NAME = "tenant name";
    public static final List<String> SAMPLE_CD_TENANT_IDS = new ArrayList<>(
            Arrays.asList(
                    "9b7e3259-aace-414c-bfd8-94daa0efefaf","9b7e3259-aace-414c-bfd8-94daa0efefaf",
                    "9b7e3259-aace-414c-bfd8-94daa0efefaf","9b7e3259-aace-414c-bfd8-94daa0efefaf",
                    "9b7e3259-aace-414c-bfd8-94daa0efefaf","9b7e3259-aace-414c-bfd8-94daa0efefaf")
    );

    public static final String SAMPLE_SCALITY_ACCOUNT_EMAIL = "9b7e3259-aace-414c-bfd8-94daa0efefaf_" +
            "9b7e3259-aace-414c-bfd8-94daa0efefaf_" +
            "9b7e3259-aace-414c-bfd8-94daa0efefaf_" +
            "9b7e3259-aace-414c-bfd8-94daa0efefaf_" +
            "9b7e3259-aace-414c-bfd8-94daa0efefaf_" +
            "9b7e3259-aace-414c-bfd8-94daa0efefaf@osis.account.com";

    public static final String SAMPLE_SCALITY_ACCOUNT_NAME ="tenant.name";



    public static OsisTenant createSampleOsisTenantObj(){
        OsisTenant osisTenantReq = new OsisTenant();
        osisTenantReq.tenantId(SAMPLE_ID);
        osisTenantReq.name(SAMPLE_TENANT_NAME);
        osisTenantReq.cdTenantIds(SAMPLE_CD_TENANT_IDS);
        osisTenantReq.active(true);
        return osisTenantReq;
    }
}
