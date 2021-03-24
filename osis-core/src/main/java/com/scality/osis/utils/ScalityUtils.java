/**
 *Copyright 2020 VMware, Inc.
 *Copyright 2021 Scality, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.utils;

import java.net.URI;

import static com.scality.osis.utils.ScalityConstants.ICON_PATH;

/**
 * Helper class with static common utils methods.
 */
public final class ScalityUtils {

    private ScalityUtils() {
    }

    public static URI getLogoUri(String domain) {
        return URI.create(domain + ICON_PATH);
    }

}
