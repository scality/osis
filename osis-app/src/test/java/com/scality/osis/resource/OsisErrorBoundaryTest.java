/**
 * Copyright 2024 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.resource;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.scality.osis.model.OsisError;
import com.scality.osis.model.exception.BadRequestException;
import com.scality.osis.model.exception.NotFoundException;
import com.scality.osis.model.exception.NotImplementedException;
import com.scality.osis.vaultadmin.impl.VaultServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.ConstraintViolationException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the single error boundary: each exception kind maps to the right HTTP
 * status and {@link OsisError} body, is logged exactly once, and at the severity
 * dictated by the kind (expected -> INFO/no-trace, genuine server fault ->
 * ERROR/trace). The handlers are invoked directly because the boundary is a plain
 * bean; controller wiring is covered separately in {@link ScalityOsisControllerTest}.
 */
class OsisErrorBoundaryTest {

    private OsisErrorBoundary boundary;
    private ListAppender<ILoggingEvent> appender;
    private Logger boundaryLogger;

    @BeforeEach
    void setUp() {
        boundary = new OsisErrorBoundary();
        boundaryLogger = (Logger) LoggerFactory.getLogger(OsisErrorBoundary.class);
        appender = new ListAppender<>();
        appender.start();
        boundaryLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        boundaryLogger.detachAppender(appender);
    }

    private ILoggingEvent onlyLogEvent() {
        assertEquals(1, appender.list.size(), "expected exactly one log line at the boundary");
        return appender.list.get(0);
    }

    private WebRequest anyRequest() {
        return new ServletWebRequest(new MockHttpServletRequest());
    }

    /** A dummy controller-method parameter, used only to build a MethodArgumentNotValidException. */
    @SuppressWarnings("unused")
    private void validatedEndpoint(final String body) {
    }

    @Test
    void testNotFoundMapsTo404LoggedOnceAtInfoWithoutTrace() {
        final ResponseEntity<OsisError> response =
                boundary.handleResponseStatus(new NotFoundException("nope"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("E_NOT_FOUND", response.getBody().getCode());
        assertEquals("nope", response.getBody().getMessage());

        final ILoggingEvent event = onlyLogEvent();
        assertEquals(Level.INFO, event.getLevel(), "4xx must be logged at INFO");
        assertNull(event.getThrowableProxy(), "4xx must not carry a stack trace");
    }

    @Test
    void testBadRequestMapsTo400LoggedOnceAtInfo() {
        final ResponseEntity<OsisError> response =
                boundary.handleResponseStatus(new BadRequestException("bad"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("E_BAD_REQUEST", response.getBody().getCode());
        assertEquals(Level.INFO, onlyLogEvent().getLevel());
    }

    @Test
    void testNotImplementedMapsTo501LoggedOnceAtInfo() {
        final ResponseEntity<OsisError> response =
                boundary.handleResponseStatus(new NotImplementedException());

        assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
        assertEquals("E_NOT_IMPLEMENTED", response.getBody().getCode());

        final ILoggingEvent event = onlyLogEvent();
        assertEquals(Level.INFO, event.getLevel(), "an unsupported API is expected, not a fault");
        assertNull(event.getThrowableProxy());
    }

    @Test
    void testRecoverableVaultNotFoundLoggedOnceAtInfoNotError() {
        // A typed 404 from the service layer (e.g. a credential lookup) is expected/handled.
        final ResponseEntity<OsisError> response =
                boundary.handleResponseStatus(new VaultServiceException(HttpStatus.NOT_FOUND, "no credential"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(Level.INFO, onlyLogEvent().getLevel(),
                "an expected/recoverable failure must not be logged at ERROR");
    }

    @Test
    void testVaultServerErrorMapsTo500LoggedOnceAtErrorWithTrace() {
        final ResponseStatusException vaultError =
                new VaultServiceException(HttpStatus.INTERNAL_SERVER_ERROR, "boom",
                        new IllegalStateException("cause"));

        final ResponseEntity<OsisError> response = boundary.handleResponseStatus(vaultError);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("E_INTERNAL_SERVER_ERROR", response.getBody().getCode());
        assertEquals("Internal server error", response.getBody().getMessage(),
                "a typed 5xx must not leak its reason to the client");

        final ILoggingEvent event = onlyLogEvent();
        assertEquals(Level.ERROR, event.getLevel(), "5xx must be logged at ERROR");
        assertNotNull(event.getThrowableProxy(), "5xx must carry a stack trace");
    }

    @Test
    void testUnexpectedExceptionReturnsGenericBodyButLogsFullDetail() {
        final ResponseEntity<OsisError> response =
                boundary.handleUnexpected(new IllegalStateException("sensitive internal detail"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("E_INTERNAL_SERVER_ERROR", response.getBody().getCode());
        assertEquals("Internal server error", response.getBody().getMessage(),
                "the client must not see internal exception detail");

        final ILoggingEvent event = onlyLogEvent();
        assertEquals(Level.ERROR, event.getLevel());
        assertNotNull(event.getThrowableProxy(), "the real exception must be logged server-side");
        assertEquals("sensitive internal detail", event.getThrowableProxy().getMessage(),
                "the full detail is preserved in the server log");
    }

    @Test
    void testFrameworkFailureMapsToItsStatusAndPreservesHeaders() {
        // ResponseEntityExceptionHandler funnels every framework request failure through
        // handleExceptionInternal at its natural 4xx status (here 405), not the 500 catch-all,
        // and the spec-required headers it sets (here Allow) survive into the response.
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ALLOW, "GET");
        final ResponseEntity<Object> response = boundary.handleExceptionInternal(
                new RuntimeException("Request method 'DELETE' not supported"), null,
                headers, HttpStatus.METHOD_NOT_ALLOWED, anyRequest());

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertEquals("GET", response.getHeaders().getFirst(HttpHeaders.ALLOW),
                "spec-required framework headers must be preserved");
        final OsisError body = (OsisError) response.getBody();
        assertEquals("E_METHOD_NOT_ALLOWED", body.getCode());

        final ILoggingEvent event = onlyLogEvent();
        assertEquals(Level.INFO, event.getLevel(), "a 4xx framework failure is expected, not a fault");
        assertNull(event.getThrowableProxy(), "4xx must not carry a stack trace");
    }

    @Test
    void testConstraintViolationMapsTo400LoggedOnceAtInfo() {
        final ResponseEntity<OsisError> response = boundary.handleConstraintViolation(
                new ConstraintViolationException("listTenants.limit: must be greater than 0", null));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("E_BAD_REQUEST", response.getBody().getCode());
        assertEquals("listTenants.limit: must be greater than 0", response.getBody().getMessage());

        final ILoggingEvent event = onlyLogEvent();
        assertEquals(Level.INFO, event.getLevel());
        assertNull(event.getThrowableProxy());
    }

    @Test
    void testBodyValidationMapsTo400WithFirstFieldMessage() throws NoSuchMethodException {
        final Method method = OsisErrorBoundaryTest.class.getDeclaredMethod("validatedEndpoint", String.class);
        final BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "tenant");
        bindingResult.addError(new FieldError("tenant", "name", "must not be blank"));
        final MethodArgumentNotValidException invalidBody =
                new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);

        final ResponseEntity<Object> response =
                boundary.handleMethodArgumentNotValid(invalidBody, new HttpHeaders(), HttpStatus.BAD_REQUEST, anyRequest());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        final OsisError body = (OsisError) response.getBody();
        assertEquals("E_BAD_REQUEST", body.getCode());
        assertEquals("name must not be blank", body.getMessage());

        final ILoggingEvent event = onlyLogEvent();
        assertEquals(Level.INFO, event.getLevel());
        assertNull(event.getThrowableProxy());
    }
}
