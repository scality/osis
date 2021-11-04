/**
 *Copyright 2020 VMware, Inc.
 *Copyright 2021 Scality, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class ScalityOsisCaps {
    @JsonProperty
    private final Map<String, Boolean> optionalApis = new HashMap<>();

    public ScalityOsisCaps() {
        ScalityOsisConstants.API_CODES.forEach(name -> optionalApis.put(name, true));
    }

    public Map<String, Boolean> getOptionalApis() {
        return optionalApis;
    }

    @JsonIgnore
    public Boolean isValid() {
        return ScalityOsisConstants.API_CODES.containsAll(optionalApis.keySet());
    }
}
