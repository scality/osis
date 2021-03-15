/**
 *Copyright 2020 VMware, Inc.
 *Copyright 2021 Scality, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.model;

import io.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

public class PageOfS3Credentials implements Page<OsisS3Credential> {
    @Valid
    private List<OsisS3Credential> items = null;

    private PageInfo pageInfo;

    public PageOfS3Credentials items(List<OsisS3Credential> items) {
        this.items = items;
        return this;
    }

    public PageOfS3Credentials addItemsItem(OsisS3Credential itemsItem) {
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
    @ApiModelProperty(value = "")
    public List<OsisS3Credential> getItems() {
        return items;
    }

    public void setItems(List<OsisS3Credential> items) {
        this.items = items;
    }

    public PageOfS3Credentials pageInfo(PageInfo pageInfo) {
        this.pageInfo = pageInfo;
        return this;
    }

    /**
     * Get pageInfo
     *
     * @return pageInfo
     */
    @ApiModelProperty(value = "")
    public PageInfo getPageInfo() {
        return pageInfo;
    }

    public void setPageInfo(PageInfo pageInfo) {
        this.pageInfo = pageInfo;
    }
}

