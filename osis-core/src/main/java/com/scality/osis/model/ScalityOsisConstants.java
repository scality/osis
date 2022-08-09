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
    public static final String HEAD_USER_API_CODE = "headUser";
    public static final String UPDATE_CREDENTIAL_STATUS_API_CODE = "updateCredentialStatus";
    public static final String GET_USAGE_API_CODE = "getUsage";
    public static final String GET_BUCKET_LIST_API_CODE = "getBucketList";
    public static final String GET_BUCKET_ID_LOGGING_API_CODE = "getBucketLoggingId";
    public static final String GET_ANONYMOUS_USER_API_CODE = "getAnonymousUser";

    public static final List<String> API_CODES = Arrays.asList(
            DELETE_TENANT_API_CODE, HEAD_USER_API_CODE, UPDATE_CREDENTIAL_STATUS_API_CODE,
            GET_USAGE_API_CODE, GET_BUCKET_LIST_API_CODE,
            GET_BUCKET_ID_LOGGING_API_CODE, GET_ANONYMOUS_USER_API_CODE);
}
