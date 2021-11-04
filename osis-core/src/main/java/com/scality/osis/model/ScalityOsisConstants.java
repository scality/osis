/**
 * Copyright 2020 VMware, Inc.
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */
package com.scality.osis.model;

import java.util.Arrays;
import java.util.List;

import static com.vmware.osis.model.OsisConstants.*;

public final class ScalityOsisConstants {
    private ScalityOsisConstants() {

    }

    public static final List<String> API_CODES = Arrays.asList(GET_TENANT_API_CODE,
            DELETE_TENANT_API_CODE, HEAD_USER_API_CODE, UPDATE_CREDENTIAL_STATUS_API_CODE,
            GET_USAGE_API_CODE, GET_BUCKET_LIST_API_CODE,
            GET_BUCKET_ID_LOGGING_API_CODE, GET_ANONYMOUS_USER_API_CODE);
}
