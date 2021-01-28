/**
 *Copyright 2020 VMware, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.vmware.osis.scality.utils;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.vmware.osis.model.Information;
import com.vmware.osis.model.Page;
import com.vmware.osis.model.PageInfo;
import com.vmware.osis.model.exception.BadRequestException;
import org.apache.commons.lang3.StringUtils;
import com.scality.vaultadmin.VaultAdmin;
import com.scality.vaultadmin.impl.VaultServiceException;

import java.net.URI;
import java.util.*;

import static com.vmware.osis.scality.utils.ScalityConstants.*;

public final class ScalityUtil {

    private ScalityUtil() {
    }

    public static String normalize(String str) {
        if(str == null){
            return null;
        } else{
           return str.replace("-", "");
        }
    }

}
