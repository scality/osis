/**
 * Copyright 2020 VMware, Inc.
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */
package com.scality.osis.model;

import java.util.Arrays;
import java.util.List;

public final class ScalityOsisConstants {

    private ScalityOsisConstants() {
    }

    public static final String DELETE_TENANT_API_CODE = "deleteTenant";
    public static final String GET_BUCKET_LIST_API_CODE = "getBucketList";
    public static final String GET_BUCKET_ID_LOGGING_API_CODE = "getBucketLoggingId";

    public static final List<String> API_CODES = Arrays.asList(
            DELETE_TENANT_API_CODE,
            GET_BUCKET_LIST_API_CODE,
            GET_BUCKET_ID_LOGGING_API_CODE
    );
}
