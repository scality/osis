/**
 * Copyright 2024 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.resource;

import com.scality.osis.model.OsisError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.validation.ConstraintViolationException;

/**
 * The single error boundary for the OSIS REST API.
 *
 * <p>Every exception that leaves a controller is translated here into a consistent
 * {@link OsisError} body and logged exactly once. There is intentionally no other
 * place that both translates and logs a request failure: the service layer either
 * recovers (and logs one concise line) or throws a typed exception for this boundary
 * to render. Letting exceptions bubble past the controller used to log them a second
 * time through Spring's default handler, with a full stack trace, even for expected
 * and recoverable conditions.
 *
 * <p>Extending {@link ResponseEntityExceptionHandler} means Spring's own request
 * failures (unsupported method, unreadable body, missing parameter, and so on) are
 * answered with their natural 4xx status instead of falling through to the catch-all
 * as a 500. All of those are routed through {@link #handleExceptionInternal} so they
 * are rendered and logged the same way as everything else.
 *
 * <p>Severity is chosen by kind, not by exception class:
 * <ul>
 *   <li>4xx (expected/handled: not found, bad request, not implemented, recoverable
 *       Vault conditions, malformed requests) is logged once at INFO with a concise
 *       message and no trace.</li>
 *   <li>5xx (genuinely unexpected) is logged once at ERROR with the stack trace, and
 *       answered with a generic message so internal detail never reaches the client.</li>
 * </ul>
 */
@RestControllerAdvice
public class OsisErrorBoundary extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(OsisErrorBoundary.class);

    /**
     * The body message returned for an unexpected server fault. The real exception is
     * logged in full server-side; the client only learns that something failed, so
     * internal detail (stack messages, library text) never leaks out.
     */
    private static final String SERVER_FAULT_MESSAGE = "Internal server error";

    /**
     * Handles all typed OSIS exceptions and Vault service errors, which all extend
     * {@link ResponseStatusException} and so already carry the intended HTTP status.
     *
     * @param e the response-status exception thrown by a controller or the service layer
     * @return the translated error response with an {@link OsisError} body
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<OsisError> handleResponseStatus(ResponseStatusException e) {
        return respond(e.getStatus(), e.getReason(), e);
    }

    /**
     * Handles parameter/path validation failures ({@code @NotNull} etc. on request
     * parameters). An expected client error mapping to 400.
     *
     * @param e the constraint-violation exception
     * @return a 400 error response with the violation message
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<OsisError> handleConstraintViolation(ConstraintViolationException e) {
        return respond(HttpStatus.BAD_REQUEST, e.getMessage(), e);
    }

    /**
     * Catch-all for anything not already typed and not a framework request failure.
     * Treated as a genuine server fault: 500, logged once with the full stack trace,
     * and answered with a generic message so no internal detail leaks to the client.
     *
     * @param e the unexpected exception
     * @return a 500 error response with a generic message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<OsisError> handleUnexpected(Exception e) {
        // respond() masks the 5xx body with SERVER_FAULT_MESSAGE; the real message is logged.
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
    }

    /**
     * Customizes the body-validation failure ({@code @Valid} on a {@code @RequestBody})
     * that Spring raises as {@link MethodArgumentNotValidException}. An expected client
     * error mapping to 400 with the first field message.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatus status, WebRequest request) {
        final String reason = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .orElse("Invalid request");
        return asObject(respond(HttpStatus.BAD_REQUEST, reason, ex), headers);
    }

    /**
     * The single funnel for every framework request failure that
     * {@link ResponseEntityExceptionHandler} maps (unsupported method, unreadable body,
     * missing parameter, and so on). Discards the framework's empty body and renders our
     * {@link OsisError} at the framework-chosen status, logged once by severity.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
            HttpHeaders headers, HttpStatus status, WebRequest request) {
        return asObject(respond(status, ex.getMessage(), ex), headers);
    }

    /**
     * Logs the failure once at a severity chosen from the status, then builds the
     * {@link OsisError} response. A genuine server fault carries the trace; an
     * expected/handled condition is a single concise line with no trace.
     *
     * <p>"Expected" is every 4xx plus {@code 501 NOT_IMPLEMENTED} (a deliberate
     * "unsupported API" answer, not a fault). Only true 5xx server errors get
     * ERROR with the stack trace.
     */
    private ResponseEntity<OsisError> respond(HttpStatus status, String reason, Exception e) {
        final String detail = (reason == null) ? status.getReasonPhrase() : reason;
        if (isServerFault(status)) {
            logger.error("{} {} -> {}", status.value(), status.getReasonPhrase(), detail, e);
        } else {
            logger.info("{} {} -> {}", status.value(), status.getReasonPhrase(), detail);
        }
        // A server fault never carries its internal detail to the client; the full detail is in
        // the log above. Expected 4xx conditions return their concise, caller-facing message.
        final String clientMessage = isServerFault(status) ? SERVER_FAULT_MESSAGE : detail;
        final OsisError errorBody = new OsisError()
                .code(toErrorCode(status))
                .message(clientMessage);
        return ResponseEntity.status(status).body(errorBody);
    }

    /**
     * Widens an {@code OsisError} response to the {@code ResponseEntity<Object>} the
     * {@link ResponseEntityExceptionHandler} overrides must return, preserving the
     * framework's spec-required headers (e.g. {@code Allow} on 405, {@code Accept} on 406).
     */
    private ResponseEntity<Object> asObject(ResponseEntity<OsisError> response, HttpHeaders headers) {
        return new ResponseEntity<>(response.getBody(), headers, response.getStatusCode());
    }

    /**
     * A genuine server fault is any 5xx except {@code 501 NOT_IMPLEMENTED}, which is a
     * deliberate, expected answer rather than something that went wrong.
     */
    private boolean isServerFault(HttpStatus status) {
        return status.is5xxServerError() && status != HttpStatus.NOT_IMPLEMENTED;
    }

    /**
     * Derives the stable {@code OsisError.code} from the HTTP status, e.g.
     * {@code BAD_REQUEST -> E_BAD_REQUEST}, matching the existing {@code E_*} convention.
     */
    private String toErrorCode(HttpStatus status) {
        return "E_" + status.name();
    }
}
