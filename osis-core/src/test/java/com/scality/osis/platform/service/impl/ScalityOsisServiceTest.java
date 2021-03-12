package com.scality.osis.platform.service.impl;

import com.scality.osis.model.*;
import com.scality.osis.model.exception.BadRequestException;
import com.scality.osis.model.exception.NotImplementedException;
import com.scality.osis.platform.ScalityTestUtils;
import com.scality.vaultclient.dto.Account;
import com.scality.vaultclient.dto.AccountData;
import com.scality.vaultclient.dto.CreateAccountRequestDTO;
import com.scality.vaultclient.dto.CreateAccountResponseDTO;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import com.scality.vaultadmin.impl.VaultAdminImpl;
import com.scality.vaultadmin.impl.VaultServiceException;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ScalityOsisServiceTest {
    private static final String TEST_TENANT_ID ="tenantId";
    private static final String TEST_STR ="value";
    private static final String NOT_IMPLEMENTED_EXCEPTION_ERR ="expected NotImplementedException";
    private static final String TEST_USER_ID ="userId";
    private static final String TEST_NAME ="name";

    //vault admin mock object
    private static VaultAdminImpl vaultAdminMock;

    private static ScalityOsisService scalityOsisServiceUnderTest;

    @BeforeAll
    private static void init(){
        vaultAdminMock = mock(VaultAdminImpl.class);
        scalityOsisServiceUnderTest = new ScalityOsisService(vaultAdminMock);
        initMocks();
    }

    private static void initMocks() {
        //initialize mock create account response
        when(vaultAdminMock.createAccount(any(CreateAccountRequestDTO.class)))
                .thenAnswer((Answer<CreateAccountResponseDTO>) invocation -> {
                    CreateAccountRequestDTO request = invocation.getArgument(0);
                    AccountData data = new AccountData();
                    data.setEmailAddress(request.getEmailAddress());
                    data.setName(request.getName());
                    if(StringUtils.isEmpty(request.getExternalAccountId())){
                        data.setId(ScalityTestUtils.SAMPLE_TENANT_ID);
                    } else{
                        data.setId(request.getExternalAccountId());
                    }
                    Account account = new Account();
                    account.setData(data);
                    CreateAccountResponseDTO response = new CreateAccountResponseDTO();
                    response.setAccount(account);

                    return response;
                });
    }

    @Test
    public void testCreateTenant(){

        // Call Scality Osis service to create a tenant
        OsisTenant osisTenantRes = scalityOsisServiceUnderTest.createTenant(ScalityTestUtils.createSampleOsisTenantObj());

        assertEquals(ScalityTestUtils.SAMPLE_ID, osisTenantRes.getTenantId());
        assertEquals(ScalityTestUtils.SAMPLE_TENANT_NAME, osisTenantRes.getName());
        assertArrayEquals(ScalityTestUtils.SAMPLE_CD_TENANT_IDS.toArray(), osisTenantRes.getCdTenantIds().toArray());
        assertTrue(osisTenantRes.getActive());
    }

    @Test
    public void testCreateTenantInactive(){
        OsisTenant osisTenantReq = ScalityTestUtils.createSampleOsisTenantObj();
        osisTenantReq.active(false);

        // Call Scality Osis service to create a tenant
        assertThrows(BadRequestException.class, () -> {
            scalityOsisServiceUnderTest.createTenant(osisTenantReq);
        });
    }

    @Test
    public void testCreateTenant500(){

        when(vaultAdminMock.createAccount(any(CreateAccountRequestDTO.class)))
                .thenAnswer((Answer<CreateAccountResponseDTO>) invocation -> {
                    throw new VaultServiceException(500, "EntityAlreadyExists");
                });

        assertThrows(VaultServiceException.class, () -> {
            scalityOsisServiceUnderTest.createTenant(ScalityTestUtils.createSampleOsisTenantObj());
        });

        //resetting mocks to original
        init();
    }

    @Test
    public void testCreateTenant400(){

        when(vaultAdminMock.createAccount(any(CreateAccountRequestDTO.class)))
                .thenAnswer((Answer<CreateAccountResponseDTO>) invocation -> {
                    throw new VaultServiceException(400, "Bad Request");
                });

        assertThrows(VaultServiceException.class, () -> {
            scalityOsisServiceUnderTest.createTenant(ScalityTestUtils.createSampleOsisTenantObj());
        });

        //resetting mocks to original
        initMocks();
    }

    @Test
    public void testQueryTenants() {
        // Setup
        final PageOfTenants expectedResult = new PageOfTenants();
        final OsisTenant osisTenant = new OsisTenant();
        osisTenant.active(false);
        osisTenant.name(TEST_NAME);
        osisTenant.setName(TEST_NAME);
        osisTenant.tenantId(TEST_TENANT_ID);
        osisTenant.setTenantId(TEST_TENANT_ID);
        osisTenant.cdTenantIds(Arrays.asList(TEST_STR));
        osisTenant.setCdTenantIds(Arrays.asList(TEST_STR));
        expectedResult.items(Arrays.asList(osisTenant));
        final OsisTenant osisTenant1 = new OsisTenant();
        osisTenant1.active(false);
        osisTenant1.name(TEST_NAME);
        osisTenant1.setName(TEST_NAME);
        osisTenant1.tenantId(TEST_TENANT_ID);
        osisTenant1.setTenantId(TEST_TENANT_ID);
        osisTenant1.cdTenantIds(Arrays.asList(TEST_STR));
        osisTenant1.setCdTenantIds(Arrays.asList(TEST_STR));
        expectedResult.setItems(Arrays.asList(osisTenant1));
        final PageInfo pageInfo = new PageInfo();
        pageInfo.limit(0L);
        pageInfo.setLimit(0L);
        pageInfo.offset(0L);
        pageInfo.setOffset(0L);
        pageInfo.total(0L);
        pageInfo.setTotal(0L);
        expectedResult.pageInfo(pageInfo);
        final PageInfo pageInfo1 = new PageInfo();
        pageInfo1.limit(0L);
        pageInfo1.setLimit(0L);
        pageInfo1.offset(0L);
        pageInfo1.setOffset(0L);
        pageInfo1.total(0L);
        pageInfo1.setTotal(0L);
        expectedResult.setPageInfo(pageInfo1);

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.queryTenants(0L, 0L, "filter"), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

    @Test
    public void testListTenants() {
        // Setup
        final PageOfTenants expectedResult = new PageOfTenants();
        final OsisTenant osisTenant = new OsisTenant();
        osisTenant.active(false);
        osisTenant.name(TEST_NAME);
        osisTenant.setName(TEST_NAME);
        osisTenant.tenantId(TEST_TENANT_ID);
        osisTenant.setTenantId(TEST_TENANT_ID);
        osisTenant.cdTenantIds(Arrays.asList(TEST_STR));
        osisTenant.setCdTenantIds(Arrays.asList(TEST_STR));
        expectedResult.items(Arrays.asList(osisTenant));
        final OsisTenant osisTenant1 = new OsisTenant();
        osisTenant1.active(false);
        osisTenant1.name(TEST_NAME);
        osisTenant1.setName(TEST_NAME);
        osisTenant1.tenantId(TEST_TENANT_ID);
        osisTenant1.setTenantId(TEST_TENANT_ID);
        osisTenant1.cdTenantIds(Arrays.asList(TEST_STR));
        osisTenant1.setCdTenantIds(Arrays.asList(TEST_STR));
        expectedResult.setItems(Arrays.asList(osisTenant1));
        final PageInfo pageInfo = new PageInfo();
        pageInfo.limit(0L);
        pageInfo.setLimit(0L);
        pageInfo.offset(0L);
        pageInfo.setOffset(0L);
        pageInfo.total(0L);
        pageInfo.setTotal(0L);
        expectedResult.pageInfo(pageInfo);
        final PageInfo pageInfo1 = new PageInfo();
        pageInfo1.limit(0L);
        pageInfo1.setLimit(0L);
        pageInfo1.offset(0L);
        pageInfo1.setOffset(0L);
        pageInfo1.total(0L);
        pageInfo1.setTotal(0L);
        expectedResult.setPageInfo(pageInfo1);

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.listTenants(0L, 0L), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

    @Test
    public void testCreateUser() {
        // Setup
        final OsisUser osisUser = new OsisUser();
        osisUser.userId(TEST_USER_ID);
        osisUser.setUserId(TEST_USER_ID);
        osisUser.canonicalUserId("canonicalUserId");
        osisUser.setCanonicalUserId("canonicalUserId");
        osisUser.tenantId(TEST_TENANT_ID);
        osisUser.setTenantId(TEST_TENANT_ID);
        osisUser.active(false);
        osisUser.setActive(false);
        osisUser.cdUserId("cdUserId");
        osisUser.setCdUserId("cdUserId");

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.createUser(osisUser), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

    @Test
    public void testQueryUsers() {
        // Setup

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.queryUsers(0L, 0L, "filter"), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

    @Test
    public void testCreateS3Credential() {
        // Setup

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.createS3Credential(TEST_TENANT_ID, TEST_USER_ID), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

    @Test
    public void testQueryS3Credentials() {
        // Setup

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.queryS3Credentials(0L, 0L, "filter"), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

    @Test
    public void testGetProviderConsoleUrl() {
        // Setup

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.getProviderConsoleUrl(), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

    @Test
    public void testGetTenantConsoleUrl() {
        // Setup

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.getTenantConsoleUrl(TEST_TENANT_ID), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

    @Test
    public void testGetS3Capabilities() {
        // Setup

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.getS3Capabilities(), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

    @Test
    public void testDeleteS3Credential() {
        // Setup

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.deleteS3Credential(TEST_TENANT_ID, TEST_USER_ID, "accessKey"), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
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
    public void testDeleteUser() {
        // Setup

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.deleteUser(TEST_TENANT_ID, TEST_USER_ID, false), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

    @Test
    public void testGetS3Credential() {
        // Setup

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.getS3Credential("accessKey"), NOT_IMPLEMENTED_EXCEPTION_ERR);

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
    public void testGetUser1() {
        // Setup

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.getUser("canonicalUserId"), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

    @Test
    public void testGetUser2() {
        // Setup

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.getUser(TEST_TENANT_ID, TEST_USER_ID), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

    @Test
    public void testHeadTenant() {
        // Setup

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.headTenant(TEST_TENANT_ID), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

    @Test
    public void testHeadUser() {
        // Setup

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.headUser(TEST_TENANT_ID, TEST_USER_ID), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results

    }

    @Test
    public void testListS3Credentials() {
        // Setup

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.listS3Credentials(TEST_TENANT_ID, TEST_USER_ID, 0L, 0L), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

    @Test
    public void testListUsers() {
        // Setup

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.listUsers(TEST_TENANT_ID, 0L, 0L), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

    @Test
    public void testUpdateUser() {
        // Setup
        final OsisUser osisUser = new OsisUser();
        osisUser.userId(TEST_USER_ID);
        osisUser.setUserId(TEST_USER_ID);
        osisUser.canonicalUserId("canonicalUserId");
        osisUser.setCanonicalUserId("canonicalUserId");
        osisUser.tenantId(TEST_TENANT_ID);
        osisUser.setTenantId(TEST_TENANT_ID);
        osisUser.active(false);
        osisUser.setActive(false);
        osisUser.cdUserId("cdUserId");
        osisUser.setCdUserId("cdUserId");

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.updateUser(TEST_TENANT_ID, TEST_USER_ID, osisUser), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

    @Test
    public void testGetInformation() {
        // Setup

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.getInformation("domain"), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

    @Test
    public void testUpdateOsisCaps() {
        // Setup
        final OsisCaps osisCaps = new OsisCaps();

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.updateOsisCaps(osisCaps), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

    @Test
    public void testGetBucketList() {
        // Setup

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.getBucketList(TEST_TENANT_ID, 0L, 0L), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results
    }

    @Test
    public void testGetOsisUsage() {
        // Setup

        // Run the test
        assertThrows(NotImplementedException.class, () -> scalityOsisServiceUnderTest.getOsisUsage(Optional.of(TEST_STR), Optional.of(TEST_STR)), NOT_IMPLEMENTED_EXCEPTION_ERR);

        // Verify the results

    }


}
