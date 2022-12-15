/**
 * Copyright 2020 VMware, Inc.
 * Copyright 2022 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;

/**
 * a standard error object
 */
@Schema(description = "a standard error object")
public class OsisError {

    @NotNull
    @JsonProperty("code")
    private String code;

    @JsonProperty("message")
    private String message;

    public OsisError code(String code) {
        this.code = code;
        return this;
    }

    @Schema(description = "", required = true, example = "E_BAD_REQUEST")
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public OsisError message(String message) {
        this.message = message;
        return this;
    }

    @Schema(description = "", example = "invalid value for the property xyz.")
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

