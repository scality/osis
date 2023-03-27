/**
 *Copyright 2020 VMware, Inc.
 *Copyright 2022 Scality, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.security.jwt.model.exception;

import org.springframework.http.HttpStatus;

import java.util.Date;

/**
 * Represents an error response, with a message, error code, HTTP status, and timestamp.
 * Used to provide a standardized format for error response
 * @see HttpStatus
 */
public class ErrorResponse {
    private final HttpStatus status;

    private final String message;

    private final ErrorCode errorCode;

    private final Date timestamp;

    /**
     * Creates an error response.
     *
     * @param message the error message
     * @param errorCode the error code
     * @param status the HTTP status
     */
    protected ErrorResponse(final String message, final ErrorCode errorCode, HttpStatus status) {
        this.message = message;
        this.errorCode = errorCode;
        this.status = status;
        this.timestamp = new Date();
    }

    /**
     * Creates an error response.
     *
     * @param message the error message
     * @param errorCode the error code
     * @param status the HTTP status
     * @return the error response
     */
    public static ErrorResponse of(final String message, final ErrorCode errorCode, HttpStatus status) {
        return new ErrorResponse(message, errorCode, status);
    }

    /**
     * @return the status
     */
    public Integer getStatus() {
        return status.value();
    }
    
    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return the status
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * @return the timestamp
     */
    public Date getTimestamp() {
        return timestamp;
    }
}
