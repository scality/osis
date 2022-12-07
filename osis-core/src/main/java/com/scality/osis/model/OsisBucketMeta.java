/**
 * Copyright 2020 VMware, Inc.
 * Copyright 2022 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */
package com.scality.osis.model;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class OsisBucketMeta {

    @NotNull
    private String name;

    @Valid
    @NotNull
    private String creationDate;

    @NotNull
    private String userId;

    public OsisBucketMeta name(String name) {
        this.name = name;
        return this;
    }

    public OsisBucketMeta creationDate(String creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    public OsisBucketMeta userId(String userId) {
        this.userId = userId;
        return this;
    }

    /**
     * bucket name
     * @return name
     */
    @Schema(description = "bucket name", required = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * bucket creation date
     * @return creationDate
     */
    @Schema(description = "bucket creation date", required = true)
    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    /**
     * user id of bucket owner
     * @return userId
     */
    @Schema(description = "user id of bucket owner", required = true)
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}

