package com.vmware.osis.scality.service.impl;

import com.scality.vaultclient.dto.Account;
import com.scality.vaultclient.dto.AccountData;
import com.scality.vaultclient.dto.CreateAccountRequestDTO;
import com.scality.vaultclient.dto.CreateAccountResponseDTO;
import com.vmware.osis.model.OsisTenant;
import com.vmware.osis.model.exception.BadRequestException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import com.scality.vaultadmin.impl.VaultAdminImpl;
import com.scality.vaultadmin.impl.VaultServiceException;

import static com.vmware.osis.scality.ScalityTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ScalityOsisServiceTest{

    //vault admin mock object
    private static VaultAdminImpl vaultAdminMock;

    private static ScalityOsisService scalityOsisService;

    @BeforeAll
    private static void init(){
        vaultAdminMock = mock(VaultAdminImpl.class);
        scalityOsisService = new ScalityOsisService(vaultAdminMock);
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
        OsisTenant osisTenantRes = scalityOsisService.createTenant(createSampleOsisTenantObj());

        assertEquals(SAMPLE_TENANT_ID, osisTenantRes.getTenantId());
        assertEquals(SAMPLE_TENANT_NAME, osisTenantRes.getName());
        assertArrayEquals(SAMPLE_CD_TENANT_IDS.toArray(), osisTenantRes.getCdTenantIds().toArray());
        assertTrue(osisTenantRes.getActive());
    }

    @Test
    public void testCreateTenantInactive(){
        OsisTenant osisTenantReq = createSampleOsisTenantObj();
        osisTenantReq.active(false);

        // Call Scality Osis service to create a tenant
        assertThrows(BadRequestException.class, () -> {
            scalityOsisService.createTenant(osisTenantReq);
        });
    }

    @Test
    public void testCreateTenant500(){

        when(vaultAdminMock.createAccount(any(CreateAccountRequestDTO.class)))
                .thenAnswer((Answer<CreateAccountResponseDTO>) invocation -> {
                    throw new VaultServiceException(500, "EntityAlreadyExists");
                });

        assertThrows(VaultServiceException.class, () -> {
            scalityOsisService.createTenant(createSampleOsisTenantObj());
        });

        //resetting mocks to original
        initMocks();
    }

    @Test
    public void testCreateTenant400(){

        when(vaultAdminMock.createAccount(any(CreateAccountRequestDTO.class)))
                .thenAnswer((Answer<CreateAccountResponseDTO>) invocation -> {
                    throw new VaultServiceException(400, "Bad Request");
                });

        assertThrows(VaultServiceException.class, () -> {
            scalityOsisService.createTenant(createSampleOsisTenantObj());
        });

        //resetting mocks to original
        initMocks();
    }
}
