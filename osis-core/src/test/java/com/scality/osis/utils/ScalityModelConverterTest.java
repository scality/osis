package com.scality.osis.utils;

import com.amazonaws.services.identitymanagement.model.*;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.vmware.osis.model.OsisTenant;
import com.vmware.osis.model.exception.BadRequestException;
import com.scality.vaultclient.dto.Account;
import com.scality.vaultclient.dto.AccountData;
import com.scality.vaultclient.dto.CreateAccountRequestDTO;
import com.scality.vaultclient.dto.CreateAccountResponseDTO;
import com.scality.vaultclient.dto.AccountSecretKeyData;
import com.scality.vaultclient.dto.GenerateAccountAccessKeyRequest;
import com.scality.vaultclient.dto.GenerateAccountAccessKeyResponse;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static com.scality.osis.utils.ScalityTestUtils.*;

import static org.junit.jupiter.api.Assertions.*;


public class ScalityModelConverterTest {

    @Test
    public void testToScalityAccountEmailTest(){
        assertEquals(SAMPLE_SCALITY_ACCOUNT_EMAIL,
                ScalityModelConverter.generateTenantEmail(SAMPLE_TENANT_NAME),"failed generateTenantEmail");
    }

    @Test
    public void testToOsisCDTenantIdsTest(){
        final List<String> result = ScalityModelConverter.toOsisCDTenantIds(SAMPLE_CUSTOM_ATTRIBUTES);
        assertTrue(SAMPLE_CD_TENANT_IDS.size() == result.size() &&
                SAMPLE_CD_TENANT_IDS.containsAll(result) && result.containsAll(SAMPLE_CD_TENANT_IDS), "failed toOsisCDTenantIdsTest");
    }

    @Test
    public void  testToScalityCreateAccountRequestTest() throws Exception {

        final OsisTenant osisTenant = new OsisTenant();
        osisTenant.tenantId(SAMPLE_TENANT_ID);
        osisTenant.name(SAMPLE_TENANT_NAME);
        osisTenant.cdTenantIds(SAMPLE_CD_TENANT_IDS);
        osisTenant.active(true);

        final CreateAccountRequestDTO createAccountRequestDTO = ScalityModelConverter.toScalityCreateAccountRequest(osisTenant);

        assertEquals(SAMPLE_SCALITY_ACCOUNT_EMAIL, createAccountRequestDTO.getEmailAddress(), "failed  toScalityCreateAccountRequestTest:getEmailAddress()");
        assertEquals(SAMPLE_TENANT_NAME, createAccountRequestDTO.getName(), "failed  toScalityCreateAccountRequestTest:getName()");
        assertEquals(SAMPLE_TENANT_ID, createAccountRequestDTO.getExternalAccountId(), "failed  toScalityCreateAccountRequestTest:getExternalAccountId()");
    }

    @Test
    public void  testToScalityCreateAccountRequestNullTenantId() {

        final OsisTenant osisTenant = new OsisTenant();
        osisTenant.tenantId(null);
        osisTenant.name(SAMPLE_TENANT_NAME);
        osisTenant.cdTenantIds(SAMPLE_CD_TENANT_IDS);
        osisTenant.active(true);

        final CreateAccountRequestDTO createAccountRequestDTO = ScalityModelConverter.toScalityCreateAccountRequest(osisTenant);

        assertNull(createAccountRequestDTO.getExternalAccountId(), "toScalityCreateAccountRequestTest should throw Null Pointer :getExternalAccountId()");
    }

    @Test
    public void  testToScalityCreateAccountRequestActiveErr(){
        final OsisTenant osisTenant = new OsisTenant();
        osisTenant.active(false);
        assertThrows(BadRequestException.class, () -> {
            ScalityModelConverter.toScalityCreateAccountRequest(osisTenant);
        }, " toScalityCreateAccountRequestActiveErr should throw BadRequestException :toScalityCreateAccountRequest()");
    }

    @Test
    public void createAccountResponseToOsisTenantTest() {

        final AccountData data = new AccountData();
        data.setName(SAMPLE_TENANT_NAME);
        data.setEmailAddress(SAMPLE_SCALITY_ACCOUNT_EMAIL);
        data.setId(SAMPLE_TENANT_ID);
        data.setCustomAttributes(SAMPLE_CUSTOM_ATTRIBUTES);
        final Account account = new Account();
        account.setData(data);
        final CreateAccountResponseDTO responseDTO = new CreateAccountResponseDTO ();
        responseDTO.setAccount(account);

        //vault specific email address format for ose-scality
        final OsisTenant osisTenant = ScalityModelConverter.toOsisTenant(responseDTO);
        assertEquals(SAMPLE_TENANT_ID, osisTenant.getTenantId(), "failed createAccountResponseToOsisTenantTest:getTenantId()");
        assertEquals(SAMPLE_TENANT_NAME, osisTenant.getName(), "failed createAccountResponseToOsisTenantTest:getName()");
        assertTrue(SAMPLE_CD_TENANT_IDS.size() == osisTenant.getCdTenantIds().size() &&
                SAMPLE_CD_TENANT_IDS.containsAll(osisTenant.getCdTenantIds())
                && osisTenant.getCdTenantIds().containsAll(SAMPLE_CD_TENANT_IDS), "failed createAccountResponseToOsisTenantTest:getCdTenantIds()");
        assertTrue(osisTenant.getActive(), "failed createAccountResponseToOsisTenantTest:getActive()");
    }

    @Test
    public void  testGetAssumeRoleRequestForAccount() {


        final AssumeRoleRequest assumeRoleRequest = ScalityModelConverter.getAssumeRoleRequestForAccount(SAMPLE_TENANT_ID, SAMPLE_ASSUME_ROLE_NAME);

        assertEquals(TEST_ROLE_ARN, assumeRoleRequest.getRoleArn(), "failed  testGetAssumeRoleRequestForAccount:getRoleArn()");
        assertTrue(assumeRoleRequest.getRoleSessionName().startsWith(TEST_SESSION_NAME_PREFIX), "failed  testGetAssumeRoleRequestForAccount:getRoleSessionName()");
    }

    @Test
    public void testToGenerateAccountAccessKeyRequest() {
        // Setup
        // Run the test
        final GenerateAccountAccessKeyRequest result = ScalityModelConverter.toGenerateAccountAccessKeyRequest(SAMPLE_TENANT_NAME, SAMPLE_DURATION_SECONDS);

        // Verify the results
        assertEquals(SAMPLE_TENANT_NAME, result.getAccountName());
        assertEquals(SAMPLE_DURATION_SECONDS, result.getDurationSeconds());
    }

    @Test
    public void testToCreateOSISRoleRequest() {
        // Setup
        // Run the test
        final CreateRoleRequest result = ScalityModelConverter.toCreateOSISRoleRequest(TEST_NAME);

        // Verify the results
        assertEquals(TEST_NAME, result.getRoleName());
        assertNotNull(result.getAssumeRolePolicyDocument());
    }

    @Test
    public void testToCreateAdminPolicyRequest() {
        // Setup
        // Run the test
        final CreatePolicyRequest result = ScalityModelConverter.toCreateAdminPolicyRequest(TEST_TENANT_ID);

        // Verify the results
        assertTrue(result.getPolicyName().contains(TEST_TENANT_ID));
        assertTrue(result.getDescription().contains(TEST_TENANT_ID));
        assertNotNull(result.getPolicyDocument());
    }

    @Test
    public void testToAttachAdminPolicyRequest() {
        // Setup
        // Run the test
        final AttachRolePolicyRequest result = ScalityModelConverter.toAttachAdminPolicyRequest(TEST_POLICY_ARN, TEST_NAME);

        // Verify the results
        assertEquals(TEST_NAME, result.getRoleName());
        assertEquals(TEST_POLICY_ARN, result.getPolicyArn());
    }

    @Test
    public void testToDeleteAccessKeyRequest() {
        // Setup
        // Run the test
        final DeleteAccessKeyRequest result = ScalityModelConverter.toDeleteAccessKeyRequest(TEST_ACCESS_KEY, TEST_NAME);

        // Verify the results
        assertEquals(TEST_NAME, result.getUserName());
        assertEquals(TEST_ACCESS_KEY, result.getAccessKeyId());
    }

    @Test
    public void testToAdminPolicyArn() {
        // Setup
        // Run the test
        final String result = ScalityModelConverter.toAdminPolicyArn(SAMPLE_TENANT_ID);

        // Verify the results
        assertTrue(result.contains(SAMPLE_TENANT_ID));
    }

    @Test
    public void testToCredentials() {
        // Setup
        final Date expirationDate = new Date(new Date().getTime() + (SAMPLE_DURATION_SECONDS * 1000L));
        final AccountSecretKeyData accountSecretKeyData = new AccountSecretKeyData().builder()
                .id(TEST_ACCESS_KEY)
                .value(TEST_SECRET_KEY)
                .userId(SAMPLE_TENANT_ID)
                .createDate(new Date())
                .lastUsedService(NA_STR)
                .lastUsedRegion(NA_STR)
                .lastUsedDate(new Date())
                .status(ACTIVE_STR)
                .notAfter(expirationDate)
                .build();

        final GenerateAccountAccessKeyResponse generateAccountAccessKeyResponse = new GenerateAccountAccessKeyResponse();
        generateAccountAccessKeyResponse.setData(accountSecretKeyData);

        // Run the test
        final Credentials result = ScalityModelConverter.toCredentials(generateAccountAccessKeyResponse);

        // Verify the results
        assertEquals(TEST_ACCESS_KEY, result.getAccessKeyId());
        assertEquals(TEST_SECRET_KEY, result.getSecretAccessKey());
        assertEquals(expirationDate, result.getExpiration());
    }
}
