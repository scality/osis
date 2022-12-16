/**
 * Copyright 2020 VMware, Inc.
 * Copyright 2022 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */
package com.scality.osis.model;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class OsisS3Credential {

    @NotNull
    private String accessKey;

    @NotNull
    private String secretKey;

    @NotNull
    private Boolean active;

    @Valid
    private String creationDate;

    private Boolean immutable;

    private String tenantId;

    private String userId;

    private String username;

    private String cdTenantId;

    private String cdUserId;

    public OsisS3Credential accessKey(String accessKey) {
        this.accessKey = accessKey;
        return this;
    }

    /**
     * S3 access key
     *
     * @return accessKey
     */
    @Schema(description = "S3 access key", requiredMode= Schema.RequiredMode.REQUIRED, example = "00e4a3d674aada749f04")
    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public OsisS3Credential secretKey(String secretKey) {
        this.secretKey = secretKey;
        return this;
    }

    /**
     * S3 secret key
     *
     * @return secretKey
     */
    @Schema(description = "S3 secret key", requiredMode= Schema.RequiredMode.REQUIRED, example = "yz8PIwNjmm2zlHX8m7st6BSKh8PCe7bqAaRGkF5K")
    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public OsisS3Credential active(Boolean active) {
        this.active = active;
        return this;
    }

    /**
     * S3 credential status
     *
     * @return active
     */
    @Schema(description = "S3 credential status", requiredMode= Schema.RequiredMode.REQUIRED, example = "true")
    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public OsisS3Credential creationDate(String creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    /**
     * S3 credential creation date
     *
     * @return creationDate
     */
    @Schema(description = "S3 credential creation date")
    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public OsisS3Credential immutable(Boolean immutable) {
        this.immutable = immutable;
        return this;
    }

    /**
     * S3 credential immutability
     *
     * @return immutable
     */
    @Schema(description = "S3 credential immutability")
    public Boolean getImmutable() {
        return immutable;
    }

    public void setImmutable(Boolean immutable) {
        this.immutable = immutable;
    }

    public OsisS3Credential tenantId(String tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    /**
     * The ID of the tenant which the S3 credential belongs to
     *
     * @return tenantId
     */
    @Schema(description = "The ID of the tenant which the S3 credential belongs to", example = "acme")
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public OsisS3Credential cdTenantId(String cdTenantId) {
        this.cdTenantId = cdTenantId;
        return this;
    }

    /**
     * The ID of the tenant which the S3 credential belongs to
     *
     * @return tenantId
     */
    @Schema(description = "The ID of the Cloud Director tenant which the S3 credential belongs to", example = "acme")
    public String getCdTenantId() {
        return cdTenantId;
    }

    public void setCdTenantId(String cdTenantId) {
        this.cdTenantId = cdTenantId;
    }

    public OsisS3Credential userId(String userId) {
        this.userId = userId;
        return this;
    }

    /**
     * The ID of the user which the S3 credential belongs to
     *
     * @return userId
     */
    @Schema(description = "The ID of the user which the S3 credential belongs to", example = "961515dd-8348-4cac-8780-5edcb8a87b58")
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public OsisS3Credential username(String username) {
        this.username = username;
        return this;
    }

    /**
     * The name of the user which the S3 credential belongs to
     *
     * @return username
     */
    @Schema(description = "The name of the user which the S3 credential belongs to", example = "rachelw")
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public OsisS3Credential cdUserId(String cdUserId) {
        this.cdUserId = cdUserId;
        return this;
    }

    @Schema(description = "The ID of the Cloud Director user which the S3 credential belongs to", example = "961515dd-8348-4cac-8780-5edcb8a87b58")
    public String getCdUserId() {
        return cdUserId;
    }

    public void setCdUserId(String cdUserId) {
        this.cdUserId = cdUserId;
    }
}