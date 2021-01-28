package com.scality.com.vmware.osis.scality.utils;

import com.scality.vaultclient.dto.Account;
import com.scality.vaultclient.dto.AccountData;
import com.scality.vaultclient.dto.CreateAccountRequestDTO;
import com.scality.vaultclient.dto.CreateAccountResponseDTO;
import com.vmware.osis.model.OsisTenant;
import com.vmware.osis.model.exception.BadRequestException;
import com.vmware.osis.scality.utils.ModelConverter;
import org.junit.jupiter.api.Test;

import java.util.*;
import static com.vmware.osis.scality.ScalityTestUtils.*;

import static org.junit.jupiter.api.Assertions.*;


public class ModelConverterTest {

    @Test
    public void toScalityTenantIdTest(){
        assertEquals(SAMPLE_SCALITY_ACCOUNT_NAME, ModelConverter.toScalityTenantId(SAMPLE_TENANT_NAME, SAMPLE_TENANT_ID));
    }

    @Test
    public void toScalityAccountEmailTest(){
        assertEquals(SAMPLE_SCALITY_ACCOUNT_EMAIL,
                ModelConverter.toScalityAccountEmail(SAMPLE_CD_TENANT_IDS));
    }

    @Test
    public void toScalityAccountEmailWith7Ids(){
        List<String> ids = new ArrayList<>();
        ids.addAll(SAMPLE_CD_TENANT_IDS);

        //Adding 7th ID to the list
        ids.add(UUID.randomUUID().toString());

        // disregard the 7th id and form email
        assertEquals(SAMPLE_SCALITY_ACCOUNT_EMAIL,
                ModelConverter.toScalityAccountEmail(ids));
    }

    @Test
    public void toOsisTenantNameTest(){
        assertEquals(SAMPLE_TENANT_NAME, ModelConverter.toOsisTenantName(SAMPLE_SCALITY_ACCOUNT_NAME));
    }

    @Test
    public void toOsisTenantIdTest(){
        AccountData data = new AccountData();
        data.setName(SAMPLE_SCALITY_ACCOUNT_NAME);
        assertEquals(SAMPLE_TENANT_ID, ModelConverter.toOsisTenantId(data));
    }

    @Test
    public void toOsisCDTenantIdsTest(){
        assertArrayEquals(SAMPLE_CD_TENANT_IDS.toArray(),
                ModelConverter.toOsisCDTenantIds(SAMPLE_SCALITY_ACCOUNT_EMAIL).toArray());
    }

    @Test
    public void toScalityAccountRequestTest() throws Exception {

        OsisTenant osisTenant = new OsisTenant();
        osisTenant.tenantId(SAMPLE_TENANT_ID);
        osisTenant.name(SAMPLE_TENANT_NAME);
        osisTenant.cdTenantIds(SAMPLE_CD_TENANT_IDS);
        osisTenant.active(true);

        CreateAccountRequestDTO createAccountRequestDTO = ModelConverter.toScalityAccountRequest(osisTenant);

        assertEquals(ModelConverter.toScalityAccountEmail(SAMPLE_CD_TENANT_IDS), createAccountRequestDTO.getEmailAddress());
        assertEquals(ModelConverter.toScalityTenantId(SAMPLE_TENANT_NAME,SAMPLE_TENANT_ID), createAccountRequestDTO.getName());
    }

    @Test
    public void toScalityAccountRequestNullTenantId() throws Exception {

        OsisTenant osisTenant = new OsisTenant();
        osisTenant.tenantId(null);
        osisTenant.name(SAMPLE_TENANT_NAME);
        osisTenant.cdTenantIds(SAMPLE_CD_TENANT_IDS);
        osisTenant.active(true);

        CreateAccountRequestDTO createAccountRequestDTO = ModelConverter.toScalityAccountRequest(osisTenant);

        assertEquals(ModelConverter.toScalityAccountEmail(SAMPLE_CD_TENANT_IDS), createAccountRequestDTO.getEmailAddress());
        assertEquals(ModelConverter.toScalityTenantId(SAMPLE_TENANT_NAME,SAMPLE_CD_TENANT_IDS.get(0)), createAccountRequestDTO.getName());
    }

    @Test
    public void toScalityAccountRequestActiveErr() throws Exception {
        OsisTenant osisTenant = new OsisTenant();
        osisTenant.active(false);
        assertThrows(BadRequestException.class, () -> {
            ModelConverter.toScalityAccountRequest(osisTenant);
        });
    }

    @Test
    public void toOsisTenantTest() throws Exception {

        AccountData data = new AccountData();
        data.setName(SAMPLE_SCALITY_ACCOUNT_NAME);
        data.setEmailAddress(SAMPLE_SCALITY_ACCOUNT_EMAIL);
        Account account = new Account();
        account.setData(data);
        CreateAccountResponseDTO responseDTO = new CreateAccountResponseDTO ();
        responseDTO.setAccount(account);

        //vault specific email address format for ose-scality
        OsisTenant osisTenant = ModelConverter.toOsisTenant(responseDTO);
        assertEquals(SAMPLE_TENANT_ID, osisTenant.getTenantId());
        assertEquals(SAMPLE_TENANT_NAME, osisTenant.getName());
        assertArrayEquals(SAMPLE_CD_TENANT_IDS.toArray(), osisTenant.getCdTenantIds().toArray());
        assertTrue(osisTenant.getActive());
    }
}
