/**
 * Copyright 2020 VMware, Inc.
 * Copyright 2022 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */
package com.scality.osis.model;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;

public class OsisUsage {

    @NotNull
    private Long bucketCount;

    @NotNull
    private Long objectCount;

    @NotNull
    private Long totalBytes;

    @NotNull
    private Long availableBytes;

    @NotNull
    private Long usedBytes;

    public OsisUsage() {
        totalBytes = -1L;
        availableBytes = -1L;
        usedBytes = -1L;
        bucketCount = -1L;
        objectCount = -1L;
    }

    public OsisUsage bucketCount(Long bucketCount) {
        this.bucketCount = bucketCount;
        return this;
    }

    /**
     * bucket count of tenant or user
     *
     * @return bucketCount
     */
    @Schema(description = "bucket count of tenant or user", required = true, example = "532")
    public Long getBucketCount() {
        return bucketCount;
    }

    public void setBucketCount(Long bucketCount) {
        this.bucketCount = bucketCount;
    }

    public OsisUsage objectCount(Long objectCount) {
        this.objectCount = objectCount;
        return this;
    }

    /**
     * object count of tenant or user
     *
     * @return objectCount
     */
    @Schema(description = "object count of tenant or user", required = true, example = "298635")
    public Long getObjectCount() {
        return objectCount;
    }

    public void setObjectCount(Long objectCount) {
        this.objectCount = objectCount;
    }

    public OsisUsage totalBytes(Long totalBytes) {
        this.totalBytes = totalBytes;
        return this;
    }

    /**
     * total storage bytes of tenant or user
     *
     * @return totalBytes
     */
    @Schema(description = "total storage bytes of tenant or user", required = true, example = "80948230763")
    public Long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(Long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public OsisUsage avaialbleBytes(Long avaialbleBytes) {
        this.availableBytes = avaialbleBytes;
        return this;
    }

    /**
     * available storage bytes of tenant or user
     *
     * @return avaialbleBytes
     */
    @Schema(description = "available storage bytes of tenant or user", required = true, example = "48193854929")
    public Long getAvailableBytes() {
        return availableBytes;
    }

    public void setAvailableBytes(Long availableBytes) {
        this.availableBytes = availableBytes;
    }

    public OsisUsage usedBytes(Long usedBytes) {
        this.usedBytes = usedBytes;
        return this;
    }

    /**
     * used storage bytes of tenant or user
     *
     * @return usedBytes
     */
    @Schema(description = "used storage bytes of tenant or user", required = true, example = "32754375834")
    public Long getUsedBytes() {
        return usedBytes;
    }

    public void setUsedBytes(Long usedBytes) {
        this.usedBytes = usedBytes;
    }

    public void consolidateUsage(OsisUsage usage) {
        this.bucketCount = Math.max(this.bucketCount, 0) + usage.getBucketCount();
        this.objectCount = Math.max(this.objectCount, 0) + usage.getObjectCount();
        this.usedBytes = Math.max(this.usedBytes, 0) + usage.getUsedBytes();
    }
}

