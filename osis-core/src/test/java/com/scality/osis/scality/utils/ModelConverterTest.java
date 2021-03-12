package com.scality.osis.scality.utils;

import com.scality.osis.model.OsisTenant;
import com.scality.osis.model.exception.BadRequestException;
import com.scality.osis.platform.utils.ModelConverter;
import com.scality.vaultclient.dto.Account;
import com.scality.vaultclient.dto.AccountData;
import com.scality.vaultclient.dto.CreateAccountRequestDTO;
import com.scality.vaultclient.dto.CreateAccountResponseDTO;
import org.junit.jupiter.api.Test;

import java.util.*;
import static com.scality.osis.platform.ScalityTestUtils.*;

import static org.junit.jupiter.api.Assertions.*;


public class ModelConverterTest {

    @Test
    public void toScalityAccountEmailTest(){
        assertEquals(SAMPLE_SCALITY_ACCOUNT_EMAIL,
                ModelConverter.toScalityAccountEmail(SAMPLE_CD_TENANT_IDS),"failed toScalityAccountEmailTest");
    }

    @Test
    public void toScalityAccountEmailWith7Ids(){
        List<String> ids = new ArrayList<>();
        ids.addAll(SAMPLE_CD_TENANT_IDS);

        //Adding 7th ID to the list
        ids.add(UUID.randomUUID().toString());

        // disregard the 7th id and form email
        assertEquals(SAMPLE_SCALITY_ACCOUNT_EMAIL,
                ModelConverter.toScalityAccountEmail(ids), "failed toScalityAccountEmailWith7Ids");
    }

    @Test
    public void toOsisCDTenantIdsTest(){
        assertArrayEquals(SAMPLE_CD_TENANT_IDS.toArray(),
                ModelConverter.toOsisCDTenantIds(SAMPLE_SCALITY_ACCOUNT_EMAIL).toArray(), "failed toOsisCDTenantIdsTest");
    }

    @Test
    public void toScalityAccountRequestTest() throws Exception {

        OsisTenant osisTenant = new OsisTenant();
        osisTenant.tenantId(SAMPLE_TENANT_ID);
        osisTenant.name(SAMPLE_TENANT_NAME);
        osisTenant.cdTenantIds(SAMPLE_CD_TENANT_IDS);
        osisTenant.active(true);

        CreateAccountRequestDTO createAccountRequestDTO = ModelConverter.toScalityAccountRequest(osisTenant);

        assertEquals(SAMPLE_SCALITY_ACCOUNT_EMAIL, createAccountRequestDTO.getEmailAddress(), "failed toScalityAccountRequestTest:getEmailAddress()");
        assertEquals(SAMPLE_TENANT_NAME, createAccountRequestDTO.getName(), "failed toScalityAccountRequestTest:getName()");
        assertEquals(SAMPLE_TENANT_ID, createAccountRequestDTO.getExternalAccountId(), "failed toScalityAccountRequestTest:getExternalAccountId()");
    }

    @Test
    public void toScalityAccountRequestNullTenantId() {

        OsisTenant osisTenant = new OsisTenant();
        osisTenant.tenantId(null);
        osisTenant.name(SAMPLE_TENANT_NAME);
        osisTenant.cdTenantIds(SAMPLE_CD_TENANT_IDS);
        osisTenant.active(true);

        CreateAccountRequestDTO createAccountRequestDTO = ModelConverter.toScalityAccountRequest(osisTenant);

        assertNull(createAccountRequestDTO.getExternalAccountId(), "toScalityAccountRequestTest should throw Null Pointer :getExternalAccountId()");
    }

    @Test
    public void toScalityAccountRequestActiveErr(){
        OsisTenant osisTenant = new OsisTenant();
        osisTenant.active(false);
        assertThrows(BadRequestException.class, () -> {
            ModelConverter.toScalityAccountRequest(osisTenant);
        }, "toScalityAccountRequestActiveErr should throw BadRequestException :toScalityAccountRequest()");
    }

    @Test
    public void createAccountResponseToOsisTenantTest() throws Exception {

        AccountData data = new AccountData();
        data.setName(SAMPLE_TENANT_NAME);
        data.setEmailAddress(SAMPLE_SCALITY_ACCOUNT_EMAIL);
        data.setId(SAMPLE_TENANT_ID);
        Account account = new Account();
        account.setData(data);
        CreateAccountResponseDTO responseDTO = new CreateAccountResponseDTO ();
        responseDTO.setAccount(account);

        //vault specific email address format for ose-scality
        OsisTenant osisTenant = ModelConverter.toOsisTenant(responseDTO);
        assertEquals(SAMPLE_TENANT_ID, osisTenant.getTenantId(), "failed createAccountResponseToOsisTenantTest:getTenantId()");
        assertEquals(SAMPLE_TENANT_NAME, osisTenant.getName(), "failed createAccountResponseToOsisTenantTest:getName()");
        assertArrayEquals(SAMPLE_CD_TENANT_IDS.toArray(), osisTenant.getCdTenantIds().toArray(), "failed createAccountResponseToOsisTenantTest:getCdTenantIds()");
        assertTrue(osisTenant.getActive(), "failed createAccountResponseToOsisTenantTest:getActive()");
    }
}
