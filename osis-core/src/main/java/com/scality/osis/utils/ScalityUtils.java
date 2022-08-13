/**
 *Copyright 2020 VMware, Inc.
 *Copyright 2021 Scality, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.utils;

import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static com.scality.osis.utils.ScalityConstants.*;

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

    public static Map<String, String> parseFilter(String filter) {
        if (StringUtils.isBlank(filter)) {
            return Collections.emptyMap();
        }

        Map<String, String> kvMap = new HashMap<>();
        Arrays.stream(StringUtils.split(filter, FILTER_SEPARATOR))
                .filter(exp -> exp.contains(FILTER_KEY_VALUE_SEPARATOR) && exp.indexOf(FILTER_KEY_VALUE_SEPARATOR) == exp.lastIndexOf(FILTER_KEY_VALUE_SEPARATOR))
                .forEach(exp -> {
                    String[] kv = StringUtils.split(exp, FILTER_KEY_VALUE_SEPARATOR);
                    if (kv.length == 2) {
                        kvMap.put(kv[0], kv[1]);
                    }

                });
        return kvMap;
    }
}
