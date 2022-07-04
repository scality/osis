/**
 *Copyright 2020 VMware, Inc.
 *Copyright 2022 Scality, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.model.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class NotFoundException extends ResponseStatusException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
