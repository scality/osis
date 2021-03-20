package com.scality.osis.utils;


import com.vmware.osis.model.OsisTenant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScalityTestUtils {

    public static final String SAMPLE_TENANT_ID = "12313265465789";
    public static final String SAMPLE_ID = "bfc0d4a51e06481cbc917e9d96e52d81";
    public static final String SAMPLE_TENANT_NAME = "tenant name";
    public static final List<String> SAMPLE_CD_TENANT_IDS = new ArrayList<>(
            Arrays.asList(
                    "9b7e3259-aace-414c-bfd8-94daa0efefaf","7b7e3259-aace-414c-bfd8-94daa0efefaf",
                    "5b7e3259-aace-414c-bfd8-94daa0efefaf","6b7e3259-aace-414c-bfd8-94daa0efefaf",
                    "4b7e3259-aace-414c-bfd8-94daa0efefaf","3b7e3259-aace-414c-bfd8-94daa0efefaf")
    );

    public static final Map<String, String> SAMPLE_CUSTOM_ATTRIBUTES  = new HashMap<>() ;
    static {
        SAMPLE_CUSTOM_ATTRIBUTES.put("cd_tenant_id%3D%3D9b7e3259-aace-414c-bfd8-94daa0efefaf", "");
        SAMPLE_CUSTOM_ATTRIBUTES.put("cd_tenant_id%3D%3D7b7e3259-aace-414c-bfd8-94daa0efefaf", "");
        SAMPLE_CUSTOM_ATTRIBUTES.put("cd_tenant_id%3D%3D5b7e3259-aace-414c-bfd8-94daa0efefaf", "");
        SAMPLE_CUSTOM_ATTRIBUTES.put("cd_tenant_id%3D%3D6b7e3259-aace-414c-bfd8-94daa0efefaf", "");
        SAMPLE_CUSTOM_ATTRIBUTES.put("cd_tenant_id%3D%3D4b7e3259-aace-414c-bfd8-94daa0efefaf", "");
        SAMPLE_CUSTOM_ATTRIBUTES.put("cd_tenant_id%3D%3D3b7e3259-aace-414c-bfd8-94daa0efefaf", "");
    }



    public static final String SAMPLE_SCALITY_ACCOUNT_EMAIL = "tenant.name@osis.scality.com";

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
