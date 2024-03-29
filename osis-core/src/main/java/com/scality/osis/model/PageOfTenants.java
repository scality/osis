/**
 * Copyright 2020 VMware, Inc.
 * Copyright 2022 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */
package com.scality.osis.model;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

public class PageOfTenants implements Page<OsisTenant> {

    @Valid
    private List<OsisTenant> items = null;

    private PageInfo pageInfo;

    public PageOfTenants items(List<OsisTenant> items) {
        this.items = items;
        return this;
    }

    public PageOfTenants addItemsItem(OsisTenant itemsItem) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(itemsItem);
        return this;
    }

    /**
     * Get items
     *
     * @return items
     */
    @Override
    @Schema(description = "")
    public List<OsisTenant> getItems() {
        return items;
    }

    @Override
    public void setItems(List<OsisTenant> items) {
        this.items = items;
    }

    @Override
    public PageOfTenants pageInfo(PageInfo pageInfo) {
        this.pageInfo = pageInfo;
        return this;
    }

    /**
     * Get pageInfo
     *
     * @return pageInfo
     */
    @Schema(description = "")
    public PageInfo getPageInfo() {
        return pageInfo;
    }

    @Override
    public void setPageInfo(PageInfo pageInfo) {
        this.pageInfo = pageInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PageOfTenants that = (PageOfTenants) o;

        if (!items.equals(that.items)) {
            return false;
        }
        return pageInfo.equals(that.pageInfo);
    }

    @Override
    public int hashCode() {
        int result = items.hashCode();
        result = 31 * result + pageInfo.hashCode();
        return result;
    }
}

