/**
 *Copyright 2020 VMware, Inc.
 *Copyright 2022 Scality, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.model;

import io.swagger.annotations.ApiModelProperty;

public class AnonymousUser {

  private String id;
  private String name;

  public AnonymousUser id(String id) {
    this.id = id;
    return this;
  }

  @ApiModelProperty(value = "anonymous user id")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public AnonymousUser name(String name) {
    this.name = name;
    return this;
  }

  @ApiModelProperty(value = "anonymous user name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
