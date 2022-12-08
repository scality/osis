/**
 * Copyright 2020 VMware, Inc.
 * Copyright 2022 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */
package com.scality.osis.model;

import io.swagger.v3.oas.annotations.media.Schema;

public class InformationServices {

    private String s3;
    private String iam;

    public InformationServices s3(String s3) {
        this.s3 = s3;
        return this;
    }

    public InformationServices iam(String iam) {
        this.iam = iam;
        return this;
    }

    /**
     * S3 URL of the storage platform
     *
     * @return s3
     */
    @Schema(description = "S3 URL of the storage platform", example = "https://s3.ceph.ose.vmware.com")
    public String getS3() {
        return s3;
    }

    public void setS3(String s3) {
        this.s3 = s3;
    }

    /**
     * IAM URL of the storage platform
     *
     * @return iam
     */
    @Schema(description = "IAM URL of the storage platform", example = "https://iam.ceph.ose.vmware.com")
    public String getIam() {
        return iam;
    }

    public void setIam(String iam) {
        this.iam = iam;
    }
}