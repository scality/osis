/**
 * Copyright 2024 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.resource;

import com.scality.osis.model.*;
import com.scality.osis.model.exception.NotImplementedException;
import com.scality.osis.service.ScalityOsisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Verifies that {@link ScalityOsisController} is a thin pass-through to
 * {@link ScalityOsisService}: each endpoint forwards its arguments and returns
 * the service result unchanged. Business logic lives in (and is tested by) the
 * service module; this test pins the controller wiring.
 */
@SuppressWarnings("PMD.TooManyMethods")
class ScalityOsisControllerTest {

    private static final String TENANT_ID = "tenant-id";
    private static final String USER_ID = "user-id";
    private static final String ACCESS_KEY = "access-key";
    private static final String FILTER = "filter";
    private static final long OFFSET = 0L;
    private static final long LIMIT = 100L;

    @Mock
    private ScalityOsisService osisService;

    @InjectMocks
    private ScalityOsisController controller;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateCredentialDelegates() {
        final OsisS3Credential expected = new OsisS3Credential();
        when(osisService.createS3Credential(TENANT_ID, USER_ID)).thenReturn(expected);

        assertSame(expected, controller.createCredential(TENANT_ID, USER_ID));
        verify(osisService).createS3Credential(TENANT_ID, USER_ID);
    }

    @Test
    void testCreateTenantDelegates() {
        final OsisTenant request = new OsisTenant();
        final OsisTenant expected = new OsisTenant();
        when(osisService.createTenant(request)).thenReturn(expected);

        assertSame(expected, controller.createTenant(request));
    }

    @Test
    void testCreateUserSetsTenantIdAndDelegates() {
        final OsisUser request = new OsisUser();
        final OsisUser expected = new OsisUser();
        when(osisService.createUser(any(OsisUser.class))).thenReturn(expected);

        assertSame(expected, controller.createUser(TENANT_ID, request));
        assertEquals(TENANT_ID, request.getTenantId());
        verify(osisService).createUser(request);
    }

    @Test
    void testDeleteCredentialDelegates() {
        controller.deleteCredential(TENANT_ID, USER_ID, ACCESS_KEY);
        verify(osisService).deleteS3Credential(TENANT_ID, USER_ID, ACCESS_KEY);
    }

    @Test
    void testDeleteTenantDelegates() {
        controller.deleteTenant(TENANT_ID, true);
        verify(osisService).deleteTenant(TENANT_ID, true);
    }

    @Test
    void testDeleteUserDelegates() {
        controller.deleteUser(TENANT_ID, USER_ID, true);
        verify(osisService).deleteUser(TENANT_ID, USER_ID, true);
    }

    @Test
    void testGetBucketListDelegates() {
        final PageOfOsisBucketMeta expected = new PageOfOsisBucketMeta();
        when(osisService.getBucketList(TENANT_ID, OFFSET, LIMIT)).thenReturn(expected);

        assertSame(expected, controller.getBucketList(TENANT_ID, OFFSET, LIMIT));
    }

    @Test
    void testGetConsoleWithTenantDelegatesToTenantUrl() {
        when(osisService.getTenantConsoleUrl(TENANT_ID)).thenReturn("tenant-url");

        assertEquals("tenant-url", controller.getConsole(Optional.of(TENANT_ID)));
        verify(osisService).getTenantConsoleUrl(TENANT_ID);
    }

    @Test
    void testGetConsoleWithoutTenantDelegatesToProviderUrl() {
        when(osisService.getProviderConsoleUrl()).thenReturn("provider-url");

        assertEquals("provider-url", controller.getConsole(Optional.empty()));
        verify(osisService).getProviderConsoleUrl();
    }

    @Test
    void testGetCredentialDelegates() {
        final OsisS3Credential expected = new OsisS3Credential();
        when(osisService.getS3Credential(TENANT_ID, USER_ID, ACCESS_KEY)).thenReturn(expected);

        assertSame(expected, controller.getCredential(TENANT_ID, USER_ID, ACCESS_KEY));
    }

    @Test
    void testGetInfoDelegatesWithRequestDomain() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURL()).thenReturn(new StringBuffer("https://osis.example.com/api/info"));
        when(request.getRequestURI()).thenReturn("/api/info");
        final Information expected = new Information();
        when(osisService.getInformation("https://osis.example.com")).thenReturn(expected);

        assertSame(expected, controller.getInfo(request));
        verify(osisService).getInformation("https://osis.example.com");
    }

    @Test
    void testGetS3CapabilitiesDelegates() {
        final OsisS3Capabilities expected = new OsisS3Capabilities();
        when(osisService.getS3Capabilities()).thenReturn(expected);

        assertSame(expected, controller.getS3Capabilities());
    }

    @Test
    void testGetTenantDelegates() {
        final OsisTenant expected = new OsisTenant();
        when(osisService.getTenant(TENANT_ID)).thenReturn(expected);

        assertSame(expected, controller.getTenant(TENANT_ID));
    }

    @Test
    void testGetUsageIsNotImplemented() {
        assertThrows(NotImplementedException.class,
                () -> controller.getUsage(Optional.of(TENANT_ID), Optional.of(USER_ID)));
    }

    @Test
    void testGetUserWithCanonicalIdDelegates() {
        final OsisUser expected = new OsisUser();
        when(osisService.getUser("canonical")).thenReturn(expected);

        assertSame(expected, controller.getUserWithCanonicalID("canonical"));
    }

    @Test
    void testGetUserWithIdDelegates() {
        final OsisUser expected = new OsisUser();
        when(osisService.getUser(TENANT_ID, USER_ID)).thenReturn(expected);

        assertSame(expected, controller.getUserWithId(TENANT_ID, USER_ID));
    }

    @Test
    void testHeadTenantDelegatesAndReturnsNull() {
        assertNull(controller.headTenant(TENANT_ID));
        verify(osisService).headTenant(TENANT_ID);
    }

    @Test
    void testHeadUserDelegates() {
        when(osisService.headUser(TENANT_ID, USER_ID)).thenReturn(true);

        assertTrue(controller.headUser(TENANT_ID, USER_ID));
    }

    @Test
    void testListCredentialsDelegates() {
        final PageOfS3Credentials expected = new PageOfS3Credentials();
        when(osisService.listS3Credentials(TENANT_ID, USER_ID, OFFSET, LIMIT)).thenReturn(expected);

        assertSame(expected, controller.listCredentials(TENANT_ID, USER_ID, OFFSET, LIMIT));
    }

    @Test
    void testListTenantsDelegates() {
        final PageOfTenants expected = new PageOfTenants();
        when(osisService.listTenants(OFFSET, LIMIT)).thenReturn(expected);

        assertSame(expected, controller.listTenants(OFFSET, LIMIT));
    }

    @Test
    void testListUsersDelegates() {
        final PageOfUsers expected = new PageOfUsers();
        when(osisService.listUsers(TENANT_ID, OFFSET, LIMIT)).thenReturn(expected);

        assertSame(expected, controller.listUsers(TENANT_ID, OFFSET, LIMIT));
    }

    @Test
    void testUpdateCredentialStatusDelegates() {
        final OsisS3Credential request = new OsisS3Credential();
        final OsisS3Credential expected = new OsisS3Credential();
        when(osisService.updateCredentialStatus(TENANT_ID, USER_ID, ACCESS_KEY, request)).thenReturn(expected);

        assertSame(expected, controller.updateCredentialStatus(TENANT_ID, USER_ID, ACCESS_KEY, request));
    }

    @Test
    void testUpdateTenantStatusDelegates() {
        final OsisTenant request = new OsisTenant();
        final OsisTenant expected = new OsisTenant();
        when(osisService.updateTenant(TENANT_ID, request)).thenReturn(expected);

        assertSame(expected, controller.updateTenantStatus(TENANT_ID, request));
    }

    @Test
    void testUpdateUserStatusDelegates() {
        final OsisUser request = new OsisUser();
        final OsisUser expected = new OsisUser();
        when(osisService.updateUser(TENANT_ID, USER_ID, request)).thenReturn(expected);

        assertSame(expected, controller.updateUserStatus(TENANT_ID, USER_ID, request));
    }

    @Test
    void testQueryTenantsDelegates() {
        final PageOfTenants expected = new PageOfTenants();
        when(osisService.queryTenants(anyLong(), anyLong(), eq(FILTER))).thenReturn(expected);

        assertSame(expected, controller.queryTenants(OFFSET, LIMIT, FILTER));
    }

    @Test
    void testQueryUsersDelegates() {
        final PageOfUsers expected = new PageOfUsers();
        when(osisService.queryUsers(anyLong(), anyLong(), eq(FILTER))).thenReturn(expected);

        assertSame(expected, controller.queryUsers(OFFSET, LIMIT, FILTER));
    }

    @Test
    void testQueryCredentialsDelegates() {
        final PageOfS3Credentials expected = new PageOfS3Credentials();
        when(osisService.queryS3Credentials(anyLong(), anyLong(), eq(FILTER))).thenReturn(expected);

        assertSame(expected, controller.queryCredentials(OFFSET, LIMIT, FILTER));
    }

    @Test
    void testGetBucketLoggingIdIsNotImplemented() {
        assertThrows(NotImplementedException.class, () -> controller.getBucketLoggingId());
    }

    @Test
    void testGetAnonymousUserDelegates() {
        final AnonymousUser expected = new AnonymousUser();
        when(osisService.getAnonymousUser()).thenReturn(expected);

        assertSame(expected, controller.getAnonymousUser());
    }

    @Test
    void testUpdateOsisCapsDelegates() {
        final ScalityOsisCaps request = new ScalityOsisCaps();
        final ScalityOsisCaps expected = new ScalityOsisCaps();
        when(osisService.updateOsisCaps(request)).thenReturn(expected);

        assertSame(expected, controller.updateOsisCaps(request));
    }
}
