package com.scality.osis.utils;

import com.vmware.osis.model.OsisTenant;
import com.vmware.osis.model.exception.BadRequestException;
import com.scality.vaultclient.dto.Account;
import com.scality.vaultclient.dto.AccountData;
import com.scality.vaultclient.dto.CreateAccountRequestDTO;
import com.scality.vaultclient.dto.CreateAccountResponseDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.scality.osis.utils.ScalityTestUtils.*;

import static org.junit.jupiter.api.Assertions.*;


public class ScalityModelConverterTest {

    @Test
    public void toScalityAccountEmailTest(){
        assertEquals(SAMPLE_SCALITY_ACCOUNT_EMAIL,
                ScalityModelConverter.generateTenantEmail(SAMPLE_TENANT_NAME),"failed generateTenantEmail");
    }

    @Test
    public void toOsisCDTenantIdsTest(){
        List<String> result = ScalityModelConverter.toOsisCDTenantIds(SAMPLE_CUSTOM_ATTRIBUTES);
        assertTrue(SAMPLE_CD_TENANT_IDS.size() == result.size() &&
                SAMPLE_CD_TENANT_IDS.containsAll(result) && result.containsAll(SAMPLE_CD_TENANT_IDS), "failed toOsisCDTenantIdsTest");
    }

    @Test
    public void  toScalityCreateAccountRequestTest() throws Exception {

        OsisTenant osisTenant = new OsisTenant();
        osisTenant.tenantId(SAMPLE_TENANT_ID);
        osisTenant.name(SAMPLE_TENANT_NAME);
        osisTenant.cdTenantIds(SAMPLE_CD_TENANT_IDS);
        osisTenant.active(true);

        CreateAccountRequestDTO createAccountRequestDTO = ScalityModelConverter.toScalityCreateAccountRequest(osisTenant);

        assertEquals(SAMPLE_SCALITY_ACCOUNT_EMAIL, createAccountRequestDTO.getEmailAddress(), "failed  toScalityCreateAccountRequestTest:getEmailAddress()");
        assertEquals(SAMPLE_TENANT_NAME, createAccountRequestDTO.getName(), "failed  toScalityCreateAccountRequestTest:getName()");
        assertEquals(SAMPLE_TENANT_ID, createAccountRequestDTO.getExternalAccountId(), "failed  toScalityCreateAccountRequestTest:getExternalAccountId()");
    }

    @Test
    public void  toScalityCreateAccountRequestNullTenantId() {

        OsisTenant osisTenant = new OsisTenant();
        osisTenant.tenantId(null);
        osisTenant.name(SAMPLE_TENANT_NAME);
        osisTenant.cdTenantIds(SAMPLE_CD_TENANT_IDS);
        osisTenant.active(true);

        CreateAccountRequestDTO createAccountRequestDTO = ScalityModelConverter.toScalityCreateAccountRequest(osisTenant);

        assertNull(createAccountRequestDTO.getExternalAccountId(), "toScalityCreateAccountRequestTest should throw Null Pointer :getExternalAccountId()");
    }

    @Test
    public void  toScalityCreateAccountRequestActiveErr(){
        OsisTenant osisTenant = new OsisTenant();
        osisTenant.active(false);
        assertThrows(BadRequestException.class, () -> {
            ScalityModelConverter.toScalityCreateAccountRequest(osisTenant);
        }, " toScalityCreateAccountRequestActiveErr should throw BadRequestException :toScalityCreateAccountRequest()");
    }

    @Test
    public void createAccountResponseToOsisTenantTest() throws Exception {

        AccountData data = new AccountData();
        data.setName(SAMPLE_TENANT_NAME);
        data.setEmailAddress(SAMPLE_SCALITY_ACCOUNT_EMAIL);
        data.setId(SAMPLE_TENANT_ID);
        data.setCustomAttributes(SAMPLE_CUSTOM_ATTRIBUTES);
        Account account = new Account();
        account.setData(data);
        CreateAccountResponseDTO responseDTO = new CreateAccountResponseDTO ();
        responseDTO.setAccount(account);

        //vault specific email address format for ose-scality
        OsisTenant osisTenant = ScalityModelConverter.toOsisTenant(responseDTO);
        assertEquals(SAMPLE_TENANT_ID, osisTenant.getTenantId(), "failed createAccountResponseToOsisTenantTest:getTenantId()");
        assertEquals(SAMPLE_TENANT_NAME, osisTenant.getName(), "failed createAccountResponseToOsisTenantTest:getName()");
        assertTrue(SAMPLE_CD_TENANT_IDS.size() == osisTenant.getCdTenantIds().size() &&
                SAMPLE_CD_TENANT_IDS.containsAll(osisTenant.getCdTenantIds())
                && osisTenant.getCdTenantIds().containsAll(SAMPLE_CD_TENANT_IDS), "failed createAccountResponseToOsisTenantTest:getCdTenantIds()");
        assertTrue(osisTenant.getActive(), "failed createAccountResponseToOsisTenantTest:getActive()");
    }
}
