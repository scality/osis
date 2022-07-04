/**
 * Copyright 2020 VMware, Inc.
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.service;

import com.scality.osis.model.OsisS3Credential;
import com.vmware.osis.service.OsisService;

public interface ScalityOsisService extends OsisService {
    OsisS3Credential getS3Credential(String tenantId, String userId, String accessKey);
}
