package com.scality.osis.service.impl;

import com.scality.osis.vaultadmin.impl.VaultServiceException;
import com.scality.vaultclient.dto.CreateAccountRequestDTO;
import com.scality.vaultclient.dto.CreateAccountResponseDTO;
import com.scality.vaultclient.dto.ListAccountsRequestDTO;
import com.scality.vaultclient.dto.ListAccountsResponseDTO;
import com.vmware.osis.model.OsisTenant;
import com.vmware.osis.model.PageOfTenants;
import com.vmware.osis.model.exception.BadRequestException;
import com.vmware.osis.model.exception.NotImplementedException;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpStatus;

import java.util.Arrays;

import static com.scality.osis.utils.ScalityConstants.CD_TENANT_ID_PREFIX;
import static com.scality.osis.utils.ScalityTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.when;

public class ScalityOsisServiceTenantTests extends BaseOsisServiceTest{

    @Test
    public void testCreateTenant(){

        // Call Scality Osis service to create a tenant
        final OsisTenant osisTenantRes = scalityOsisServiceUnderTest.createTenant(createSampleOsisTenantObj());

        assertEquals(SAMPLE_ID, osisTenantRes.getTenantId());
        assertEquals(SAMPLE_TENANT_NAME, osisTenantRes.getName());
        assertTrue(SAMPLE_CD_TENANT_IDS.size() == osisTenantRes.getCdTenantIds().size() &&
                SAMPLE_CD_TENANT_IDS.containsAll(osisTenantRes.getCdTenantIds()) && osisTenantRes.getCdTenantIds().containsAll(SAMPLE_CD_TENANT_IDS));
        assertTrue(osisTenantRes.getActive());
    }

    @Test
    public void testCreateTenantInactive(){
        final OsisTenant osisTenantReq = createSampleOsisTenantObj();
        osisTenantReq.active(false);

        // Call Scality Osis service to create a tenant
        assertThrows(BadRequestException.class, () -> {
            scalityOsisServiceUnderTest.createTenant(osisTenantReq);
        });
    }

    @Test
    public void testCreateTenant409() throws Exception {

        when(vaultAdminMock.createAccount(any(CreateAccountRequestDTO.class)))
                .thenAnswer((Answer<CreateAccountResponseDTO>) invocation -> {
                    throw new VaultServiceException(HttpStatus.CONFLICT, "EntityAlreadyExists");
                });

        assertThrows(VaultServiceException.class, () -> {
            scalityOsisServiceUnderTest.createTenant(createSampleOsisTenantObj());
        });

    }

    @Test
    public void testCreateTenant400(){

        when(vaultAdminMock.createAccount(any(CreateAccountRequestDTO.class)))
                .thenAnswer((Answer<CreateAccountResponseDTO>) invocation -> {
                    throw new VaultServiceException(HttpStatus.BAD_REQUEST, "Bad Request");
                });

        assertThrows(VaultServiceException.class, () -> {
            scalityOsisServiceUnderTest.createTenant(createSampleOsisTenantObj());
        });

    }

    @Test
    public void testListTenants() {
        // Setup
        final long offset = 0L;
        final long limit = 1000L;
        // Run the test
        // Call Scality Osis service to list tenants
        final PageOfTenants response = scalityOsisServiceUnderTest.listTenants(offset, limit);

        // Verify the results
        assertEquals(limit, response.getPageInfo().getTotal());
        assertEquals(offset, response.getPageInfo().getOffset());
        assertEquals(limit, response.getPageInfo().getLimit());
        assertEquals((int)limit, response.getItems().size());
        assertNotNull(response.getItems().get(0).getCdTenantIds());
    }

    @Test
    public void testListTenantsOffset() {
        // Setup
        final long offset = 2000L;
        final long limit = 1000L;
        // Run the test
        // Call Scality Osis service to list tenants
        final PageOfTenants response = scalityOsisServiceUnderTest.listTenants(offset, limit);


        // Verify the results
        assertEquals(limit, response.getPageInfo().getTotal());
        assertEquals(offset, response.getPageInfo().getOffset());
        assertEquals(limit, response.getPageInfo().getLimit());
        assertEquals((int)limit, response.getItems().size());
        assertNotNull(response.getItems().get(0).getCdTenantIds());
    }

    @Test
    public void testListTenantsErr() {
        // Setup
        final long offset = 0L;
        final long limit = 1000L;
        when(vaultAdminMock.listAccounts(anyLong(),any(ListAccountsRequestDTO.class)))
                .thenAnswer((Answer<ListAccountsResponseDTO>) invocation -> {
                    throw new VaultServiceException(HttpStatus.BAD_REQUEST, "Requested offset is outside the total available items");
                });

        final PageOfTenants response = scalityOsisServiceUnderTest.listTenants(offset, limit);

        // Verify the results
        assertEquals(0L, response.getPageInfo().getTotal());
        assertEquals(offset, response.getPageInfo().getOffset());
        assertEquals(limit, response.getPageInfo().getLimit());
        assertEquals(0L, response.getItems().size());

    }

    @Test
    public void testQueryTenants() {
        // Setup
        final long offset = 0L;
        final long limit = 1000L;
        final String filter = CD_TENANT_ID_PREFIX + SAMPLE_CD_TENANT_ID;

        // Run the test
        // Call Scality Osis service to query tenants
        final PageOfTenants response = scalityOsisServiceUnderTest.queryTenants(offset, limit, filter);

        // Verify the results
        assertEquals(1, response.getPageInfo().getTotal());
        assertEquals(offset, response.getPageInfo().getOffset());
        assertEquals(limit, response.getPageInfo().getLimit());
        assertEquals(1, response.getItems().size());
        assertEquals(SAMPLE_CD_TENANT_ID, response.getItems().get(0).getCdTenantIds().get(0));
    }

    @Test
    public void testQueryTenantsWithNonUUID() {
        // Setup
        final long offset = 0L;
        final long limit = 1000L;
        final String filter = CD_TENANT_ID_PREFIX + SAMPLE_TENANT_ID;

        // Run the test
        // Call Scality Osis service to query tenants
        final PageOfTenants response = scalityOsisServiceUnderTest.queryTenants(offset, limit, filter);

        // Verify the results
        assertEquals(1, response.getPageInfo().getTotal());
        assertEquals(offset, response.getPageInfo().getOffset());
        assertEquals(limit, response.getPageInfo().getLimit());
        assertEquals(1, response.getItems().size());
        assertEquals(SAMPLE_CD_TENANT_ID, response.getItems().get(0).getCdTenantIds().get(0));
    }

    @Test
    public void testQueryTenantsOffset() {
        // Setup
        final long offset = 2000L;
        final long limit = 1000L;
        final String filter = CD_TENANT_ID_PREFIX + SAMPLE_CD_TENANT_ID;
        // Run the test
        // Call Scality Osis service to list tenants
        final PageOfTenants response = scalityOsisServiceUnderTest.queryTenants(offset, limit, filter);


        // Verify the results
        assertEquals(1, response.getPageInfo().getTotal());
        assertEquals(offset, response.getPageInfo().getOffset());
        assertEquals(limit, response.getPageInfo().getLimit());
        assertEquals(1, response.getItems().size());
        assertEquals(SAMPLE_CD_TENANT_ID, response.getItems().get(0).getCdTenantIds().get(0));
    }

    @Test
    public void testQueryTenantsNoFilter() {
        // Setup
        final long offset = 0L;
        final long limit = 1000L;
        final String filter = "";

        // Run the test
        // Call Scality Osis service to query tenants
        final PageOfTenants response = scalityOsisServiceUnderTest.queryTenants(offset, limit, filter);

        // Should return the response as list tenants
        assertEquals(limit, response.getPageInfo().getTotal());
        assertEquals(offset, response.getPageInfo().getOffset());
        assertEquals(limit, response.getPageInfo().getLimit());
        assertEquals((int)limit, response.getItems().size());
        assertNotNull(response.getItems().get(0).getCdTenantIds());
    }

    @Test
    public void testQueryTenantsErr() {
        // Setup
        final long offset = 3000L;
        final long limit = 1000L;
        final String filter = CD_TENANT_ID_PREFIX + SAMPLE_CD_TENANT_ID;

        when(vaultAdminMock.listAccounts(anyLong(),any(ListAccountsRequestDTO.class)))
                .thenAnswer((Answer<ListAccountsResponseDTO>) invocation -> {
                    throw new VaultServiceException(HttpStatus.BAD_REQUEST, "Requested offset is outside the total available items");
                });

        final PageOfTenants response = scalityOsisServiceUnderTest.queryTenants(offset, limit, filter);

        // Verify the results
        assertEquals(0L, response.getPageInfo().getTotal());
        assertEquals(offset, response.getPageInfo().getOffset());
        assertEquals(limit, response.getPageInfo().getLimit());
        assertEquals(0L, response.getItems().size());

    }

    @Test
    public void testDeleteTenant() {
        // Setup

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.deleteTenant(TEST_TENANT_ID, false), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

    @Test
    public void testUpdateTenant() {
        // Setup
        final OsisTenant osisTenant = new OsisTenant();
        osisTenant.active(false);
        osisTenant.name(TEST_NAME);
        osisTenant.setName(TEST_NAME);
        osisTenant.tenantId(TEST_TENANT_ID);
        osisTenant.setTenantId(TEST_TENANT_ID);
        osisTenant.cdTenantIds(Arrays.asList(TEST_STR));
        osisTenant.setCdTenantIds(Arrays.asList(TEST_STR));

        final OsisTenant expectedResult = new OsisTenant();
        expectedResult.active(false);
        expectedResult.name(TEST_NAME);
        expectedResult.setName(TEST_NAME);
        expectedResult.tenantId(TEST_TENANT_ID);
        expectedResult.setTenantId(TEST_TENANT_ID);
        expectedResult.cdTenantIds(Arrays.asList(TEST_STR));
        expectedResult.setCdTenantIds(Arrays.asList(TEST_STR));

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.updateTenant(TEST_TENANT_ID, osisTenant), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

    @Test
    public void testGetTenant() {
        // Setup
        final OsisTenant expectedResult = new OsisTenant();
        expectedResult.active(false);
        expectedResult.name(TEST_NAME);
        expectedResult.setName(TEST_NAME);
        expectedResult.tenantId(TEST_TENANT_ID);
        expectedResult.setTenantId(TEST_TENANT_ID);
        expectedResult.cdTenantIds(Arrays.asList(TEST_STR));
        expectedResult.setCdTenantIds(Arrays.asList(TEST_STR));

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.getTenant(TEST_TENANT_ID), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

    @Test
    public void testHeadTenant() {
        // Setup

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.headTenant(TEST_TENANT_ID), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

}
