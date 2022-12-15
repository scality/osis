/**
 * Copyright 2020 VMware, Inc.
 * Copyright 2022 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */
package com.scality.osis.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.scality.osis.validation.Update;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import java.util.List;

public class OsisUser {

    private String userId;

    private String canonicalUserId;

    private String tenantId;

    @NotNull(groups = {Update.class})
    private Boolean active;

    private String cdUserId;

    private String cdTenantId;

    private String username;

    @Email
    private String email;

    @JsonIgnore
    private List<OsisS3Credential> osisS3Credentials;

    /**
     * user role
     */
    public enum RoleEnum {
        PROVIDER_ADMIN("PROVIDER_ADMIN"),
        TENANT_ADMIN("TENANT_ADMIN"),
        TENANT_USER("TENANT_USER"),
        ANONYMOUS("ANONYMOUS"),
        UNKNOWN("UNKNOWN");

        private String value;

        RoleEnum(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        @JsonCreator
        public static RoleEnum fromValue(String value) {
            for (RoleEnum b : RoleEnum.values()) {
                if (b.value.equals(value)) {
                    return b;
                }
            }
            throw new IllegalArgumentException("Unexpected value '" + value + "'");
        }
    }

    private RoleEnum role;

    public OsisUser userId(String userId) {
        this.userId = userId;
        return this;
    }

    /**
     * user id
     *
     * @return userId
     */
    @Schema(description = "user id", requiredMode= Schema.RequiredMode.REQUIRED, example = "rachelw")
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public OsisUser canonicalUserId(String canonicalUserId) {
        this.canonicalUserId = canonicalUserId;
        return this;
    }

    /**
     * canonical user id
     *
     * @return canonicalUserId
     */
    @Schema(description = "canonical user id", requiredMode= Schema.RequiredMode.REQUIRED, example = "68fb0f20-4a0c-4036-a584-cc3ee421093f")
    public String getCanonicalUserId() {
        return canonicalUserId;
    }

    public void setCanonicalUserId(String canonicalUserId) {
        this.canonicalUserId = canonicalUserId;
    }

    public OsisUser tenantId(String tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    /**
     * id of the tenant which the user belongs to
     *
     * @return tenantId
     */
    @Schema(description = "id of the tenant which the user belongs to", requiredMode= Schema.RequiredMode.REQUIRED, example = "bb8287a9-874e-46d2-abbd-58278e1ac046")
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public OsisUser active(Boolean active) {
        this.active = active;
        return this;
    }

    /**
     * user status
     *
     * @return active
     */
    @Schema(description = "user status", requiredMode= Schema.RequiredMode.REQUIRED)
    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public OsisUser cdUserId(String cdUserId) {
        this.cdUserId = cdUserId;
        return this;
    }

    /**
     * Cloud Director user id
     *
     * @return cdUserId
     */
    @Schema(description = "Cloud Director user id", requiredMode= Schema.RequiredMode.REQUIRED, example = "rachelw")
    public String getCdUserId() {
        return cdUserId;
    }

    public void setCdUserId(String cdUserId) {
        this.cdUserId = cdUserId;
    }

    public OsisUser cdTenantId(String cdTenantId) {
        this.cdTenantId = cdTenantId;
        return this;
    }

    /**
     * id of Cloud Director tenant which the user belongs to
     *
     * @return cdTenantId
     */
    @Schema(description = "id of Cloud Director tenant which the user belongs to", requiredMode= Schema.RequiredMode.REQUIRED, example = "40b97e3c-c3b1-4251-b7de-e9637324683f")
    public String getCdTenantId() {
        return cdTenantId;
    }

    public void setCdTenantId(String cdTenantId) {
        this.cdTenantId = cdTenantId;
    }

    public OsisUser displayName(String displayName) {
        this.username = displayName;
        return this;
    }

    /**
     * user display name
     *
     * @return displayName
     */
    @Schema(description = "user display name")
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public OsisUser email(String email) {
        this.email = email;
        return this;
    }

    /**
     * user email
     *
     * @return email
     */
    @Schema(description = "user email", example = "rachelw@acme.com")
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public OsisUser role(RoleEnum role) {
        this.role = role;
        return this;
    }

    /**
     * user role
     *
     * @return role
     */
    @Schema(description = "user role")
    public RoleEnum getRole() {
        return role;
    }

    public void setRole(RoleEnum role) {
        this.role = role;
    }

    public OsisUser osisS3Credentials(List<OsisS3Credential> osisS3Credentials) {
        this.osisS3Credentials = osisS3Credentials;
        return this;
    }

    public List<OsisS3Credential> getOsisS3Credentials() {
        return osisS3Credentials;
    }

    public void setOsisS3Credentials(List<OsisS3Credential> osisS3Credentials) {
        this.osisS3Credentials = osisS3Credentials;
    }
}