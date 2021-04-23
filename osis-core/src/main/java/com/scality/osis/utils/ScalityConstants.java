/**
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.utils;

public final class ScalityConstants {

    private ScalityConstants() {
    }

    public static final String TENANT_EMAIL_SUFFIX = "@osis.scality.com";

    public static final String CD_TENANT_ID_PREFIX = "cd_tenant_id==";

    public static final String ROLE_ARN_FORMAT = "arn:aws:iam::$ACCOUNTID:role/$ROLENAME";

    public static final String ACCOUNT_ID_REGEX = "$ACCOUNTID";

    public static final String ROLE_NAME_REGEX = "$ROLENAME";

    public static final String ROLE_SESSION_NAME_PREFIX = "ROLE_SESSION_";

    public static final String SEPARATOR = ".";

    public static final String ICON_PATH = "/scalitylogo.png";

    public static final String IAM_PREFIX = "/";
}
