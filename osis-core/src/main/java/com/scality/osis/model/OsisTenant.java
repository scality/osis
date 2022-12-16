/**
 * Copyright 2020 VMware, Inc.
 * Copyright 2022 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */
package com.scality.osis.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

public class OsisTenant {

    //  @NotNull
    private boolean active;

    //  @NotNull
    private String name;

    //  @NotNull
    private String tenantId;

    private List<String> cdTenantIds;

    public OsisTenant active(boolean active) {
        this.active = active;
        return this;
    }

    public boolean getActive() {
        return this.active;
    }

    public OsisTenant name(String name) {
        this.name = name;
        return this;
    }

    /**
     * tenant name
     *
     * @return name
     */
    @Schema(description = "tenant name", requiredMode= Schema.RequiredMode.REQUIRED, example = "ACME")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * tenant status
     *
     * @return active
     */
    public OsisTenant tenantId(String tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    /**
     * tenant id
     *
     * @return tenantId
     */
    @Schema(description = "tenant id", requiredMode= Schema.RequiredMode.REQUIRED, example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public OsisTenant cdTenantIds(List<String> cdTenantIds) {
        this.cdTenantIds = cdTenantIds;
        return this;
    }

    public OsisTenant addCdTenantId(String cdTenantId) {
        if (this.cdTenantIds == null) {
            this.cdTenantIds = new ArrayList<>();
        }
        this.cdTenantIds.add(cdTenantId);
        return this;
    }

    /**
     * Cloud Director tenant id
     *
     * @return cdTenantIds
     */
    @Schema(description = "Cloud Director tenant id", requiredMode= Schema.RequiredMode.REQUIRED, example = "8daca9a9-5b11-4f63-9c52-953a2ef77739")
    public List<String> getCdTenantIds() {
        return cdTenantIds;
    }

    public void setCdTenantIds(List<String> cdTenantIds) {
        this.cdTenantIds = cdTenantIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        OsisTenant that = (OsisTenant) o;

        if (active != that.active) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        if (tenantId != null ? !tenantId.equals(that.tenantId) : that.tenantId != null) {
            return false;
        }
        return cdTenantIds != null ? cdTenantIds.equals(that.cdTenantIds) : that.cdTenantIds == null;
    }

    @Override
    public int hashCode() {
        int result = (active ? 1 : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (tenantId != null ? tenantId.hashCode() : 0);
        result = 31 * result + (cdTenantIds != null ? cdTenantIds.hashCode() : 0);
        return result;
    }
}