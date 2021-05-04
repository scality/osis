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

    public static final String NO_SUCH_ENTITY_ERR = "NoSuchEntity";

    public static final String ROLE_DOES_NOT_EXIST_ERR = "Role does not exist";

    public static final String DEFAULT_ACCOUNT_AK_DURATION_SECONDS = "120";

    public static final String DEFAULT_ADMIN_POLICY_DESCRIPTION = "This is a admin role policy created for OSIS to IAM actions using AssumeRole for the $ACCOUNTID account";

    public static final String DEFAULT_OSIS_ROLE_INLINE_POLICY = "{\n" +
            "        \"Version\": \"2012-10-17\",\n" +
            "        \"Statement\": [{\n" +
            "                \"Effect\": \"Allow\",\n" +
            "                \"Principal\": \"*\",\n" +
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

    // Async threadpool executor parameters
    public static final String ASYNC_THREADPOOL_NAME_PREFIX = "async-thread-";
    public static final String DEFAULT_ASYNC_EXECUTOR_CORE_POOL_SIZE = "10";
    public static final String DEFAULT_ASYNC_EXECUTOR_MAX_POOL_SIZE = "10";
    public static final String DEFAULT_ASYNC_EXECUTOR_QUEUE_CAPACITY = "500";

    public static final String USER_POLICY_ARN_REGEX = "arn:aws:iam::$ACCOUNTID:policy/userPolicy@$ACCOUNTID";

    public static final String USER_POLICY_NAME_REGEX = "userPolicy@$ACCOUNTID";

    public static final String DEFAULT_USER_POLICY_DESCRIPTION = "This is a common user policy created by OSIS for all the users belonging to the $ACCOUNTID account";

    public static final String DEFAULT_USER_POLICY_DOCUMENT = "{\n  " +
                                                        "\"Version\": \"2012-10-17\",\n  " +
                                                        "\"Statement\": [\n    {\n      " +
                                                                                    "\"Effect\": \"Allow\",\n      " +
                                                                                    "\"Action\": [\n        " +
                                                                                        "\"s3:*\",\n        " +
                                                                                        "\"kms:*\"\n      ],\n      " +
                                                                                    "\"Resource\": \"*\"\n    }\n  " +
                                                                        "]\n}";
}
