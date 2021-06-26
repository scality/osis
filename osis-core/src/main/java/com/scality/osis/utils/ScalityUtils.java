/**
 *Copyright 2020 VMware, Inc.
 *Copyright 2021 Scality, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.utils;

import java.net.URI;
import java.util.regex.Pattern;

import static com.scality.osis.utils.ScalityConstants.ICON_PATH;
import static com.scality.osis.utils.ScalityConstants.UUID_REGEX;

/**
 * Helper class with static common utils methods.
 */
public final class ScalityUtils {
    private final static Pattern UUID_REGEX_PATTERN =
            Pattern.compile(UUID_REGEX);

    private ScalityUtils() {
    }

    public static URI getLogoUri(String domain) {
        return URI.create(domain + ICON_PATH);
    }

    public static boolean isValidUUID(String str) {
        if (str == null) {
            return false;
        }
        return UUID_REGEX_PATTERN.matcher(str).matches();
    }

}
