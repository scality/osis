/**
 * Copyright 2020 VMware, Inc.
 * Copyright 2022 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class Information {
    @NotNull
    private String platformName;

    @NotNull
    private String platformVersion;

    @NotNull
    private String apiVersion;

    /**
     * Gets or Sets status
     */
    public enum StatusEnum {
        NORMAL("NORMAL"),
        WARNING("WARNING"),
        ERROR("ERROR"),
        UNKNOWN("UNKNOWN");

        private String value;

        StatusEnum(String value) {
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
        public static StatusEnum fromValue(String value) {
            for (StatusEnum b : StatusEnum.values()) {
                if (b.value.equals(value)) {
                    return b;
                }
            }
            throw new IllegalArgumentException("Unexpected value '" + value + "'");
        }
    }

    @NotNull
    private StatusEnum status;

    @Valid
    @NotNull
    private List<String> notImplemented = new ArrayList<>();

    private URI logoUri;

    /**
     * Gets or Sets authModes
     */
    public enum AuthModesEnum {
        BASIC("Basic"),
        BEARER("Bearer");;

        private String value;

        AuthModesEnum(String value) {
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
        public static AuthModesEnum fromValue(String value) {
            for (AuthModesEnum b : AuthModesEnum.values()) {
                if (b.value.equals(value)) {
                    return b;
                }
            }
            throw new IllegalArgumentException("Unexpected value '" + value + "'");
        }
    }

    @Valid
    private List<AuthModesEnum> authModes = null;

    private InformationServices services;

    @Valid
    private List<String> regions = null;

    @Valid
    private List<String> storageClasses = null;

    public Information platformName(String platformName) {
        this.platformName = platformName;
        return this;
    }

    /**
     * name of the storage platform
     *
     * @return platformName
     */
    @Schema(description = "Name of the storage platform", required = true, example = "ceph")
    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public Information apiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
        return this;
    }

    /**
     * name of the storage platform
     *
     * @return platformName
     */
    @Schema(description = "Version of the storage platform", required = true, example = "15.2.3")
    public String getPlatformVersion() {
        return platformVersion;
    }

    public void setPlatformVersion(String platformVersion) {
        this.platformVersion = platformVersion;
    }

    public Information platformVersion(String platformVersion) {
        this.platformVersion = platformVersion;
        return this;
    }

    /**
     * OSIS version the REST services complying with
     *
     * @return apiVersion
     */
    @Schema(description = "OSIS version the REST services complying with", required = true, example = "1.0")
    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public Information status(StatusEnum status) {
        this.status = status;
        return this;
    }

    /**
     * Get status
     *
     * @return status
     */
    @Schema(description = "Platform status", required = true)
    public StatusEnum getStatus() {
        return status;
    }

    public void setStatus(StatusEnum status) {
        this.status = status;
    }

    public Information notImplemented(List<String> notImplemented) {
        this.notImplemented = notImplemented;
        return this;
    }

    public Information addNotImplementedItem(String notImplementedItem) {
        this.notImplemented.add(notImplementedItem);
        return this;
    }

    /**
     * the operation id array of optional OSIS APIs which is not implemented
     *
     * @return notImplemented
     */
    @ArraySchema(schema = @Schema(description = "List of OSIS endpoints which are not implemented", required = true, example = """
            ["getUsage","getConsole"] """))
    public List<String> getNotImplemented() {
        return notImplemented;
    }

    public void setNotImplemented(List<String> notImplemented) {
        this.notImplemented = notImplemented;
    }

    public Information logoUri(URI logoUri) {
        this.logoUri = logoUri;
        return this;
    }

    /**
     * uri of the platform logo so that OSE can use it on UI
     *
     * @return logoUri
     */
    @Schema(description = "URI of the platform logo so that OSE can use it on UI", example = "https://ceph.ose.vmware.com/static/images/ceph.svg")
    @Valid
    public URI getLogoUri() {
        return logoUri;
    }

    public void setLogoUri(URI logoUri) {
        this.logoUri = logoUri;
    }

    public Information authModes(List<AuthModesEnum> authModes) {
        this.authModes = authModes;
        return this;
    }

    public Information addAuthModesItem(AuthModesEnum authModesItem) {
        if (this.authModes == null) {
            this.authModes = new ArrayList<>();
        }
        this.authModes.add(authModesItem);
        return this;
    }

    /**
     * Get authModes
     *
     * @return authModes
     */
    @ArraySchema(schema = @Schema(description = "Authentication modes"))
    public List<AuthModesEnum> getAuthModes() {
        return authModes;
    }

    public void setAuthModes(List<AuthModesEnum> authModes) {
        this.authModes = authModes;
    }

    public Information services(InformationServices services) {
        this.services = services;
        return this;
    }

    /**
     * Get services
     *
     * @return services
     */
    @Schema(description = "Services information")
    @Valid
    public InformationServices getServices() {
        return services;
    }

    public void setServices(InformationServices services) {
        this.services = services;
    }

    public Information regions(List<String> regions) {
        this.regions = regions;
        return this;
    }

    public Information addRegionsItem(String regionsItem) {
        if (this.regions == null) {
            this.regions = new ArrayList<>();
        }
        this.regions.add(regionsItem);
        return this;
    }

    /**
     * Get regions
     *
     * @return regions
     */
    @Schema(description = "List of regions")
    public List<String> getRegions() {
        return regions;
    }

    public void setRegions(List<String> regions) {
        this.regions = regions;
    }

    public Information storageClasses(List<String> storageClasses) {
        this.storageClasses = storageClasses;
        return this;
    }

    public Information addStorageClassesItem(String storageClassesItem) {
        if (this.storageClasses == null) {
            this.storageClasses = new ArrayList<>();
        }
        this.storageClasses.add(storageClassesItem);
        return this;
    }

    /**
     * Get storageClasses
     *
     * @return storageClasses
     */
    @Schema(description = "List of storage classes")
    public List<String> getStorageClasses() {
        return storageClasses;
    }

    public void setStorageClasses(List<String> storageClasses) {
        this.storageClasses = storageClasses;
    }
}

