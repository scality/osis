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

    public static final String DEFAULT_ACCOUNT_AK_DURATION_SECONDS = "120";

    public static final String DEFAULT_ADMIN_POLICY_DESCRIPTION = "This is a admin role policy created for OSIS to IAM actions using AssumeRole for the $ACCOUNTID account";

    public static final String DEFAULT_OSIS_ROLE_INLINE_POLICY = "{\n" +
                                                                    "        \"Version\": \"2012-10-17\",\n" +
                                                                    "        \"Statement\": [{\n" +
                                                                    "                \"Effect\": \"Allow\",\n" +
                                                                    "                \"Principal\": {\n" +
                                                                    "                        \"Service\": \"backbeat\"\n" + // TODO: change backbeat to OSIS
                                                                    "                },\n" +
                                                                    "                \"Action\": \"sts:AssumeRole\"\n" +
                                                                    "        }]\n" +
                                                                    "}";

    public static final String DEFAULT_ADMIN_POLICY_DOCUMENT  = "{\n" +
                                                                "   \"Version\":\"2012-10-17\",\n" +
                                                                "   \"Statement\":[\n" +
                                                                "      {\n" +
                                                                "         \"Effect\":\"Allow\",\n" +
                                                                "         \"Action\":[\n" +
                                                                "            \"iam:*\"\n" +
                                                                "         ],\n" +
                                                                "         \"Resource\":\"*\"\n" +
                                                                "      }\n" +
                                                                "   ]\n" +
                                                                "}";

    public static final String ACCOUNT_ADMIN_POLICY_ARN_REGEX = "arn:aws:iam::$ACCOUNTID:policy/adminPolicy@$ACCOUNTID";

    public static final String ACCOUNT_ADMIN_POLICY_NAME_REGEX = "adminPolicy@$ACCOUNTID";
}
