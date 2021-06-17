package com.scality.osis.utils;

import com.amazonaws.services.identitymanagement.model.*;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.vmware.osis.model.*;
import com.vmware.osis.model.exception.BadRequestException;
import com.scality.vaultclient.dto.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.scality.osis.utils.ScalityConstants.DEFAULT_USER_POLICY_DOCUMENT;
import static com.scality.osis.utils.ScalityConstants.MASKED_SENSITIVE_DATA_STR;
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

    @Test
    public void testToGetAccountRequestWithID() {
        // Setup
        // Run the test
        final GetAccountRequestDTO result = ScalityModelConverter.toGetAccountRequestWithID(TEST_TENANT_ID);

        // Verify the results
        assertEquals(TEST_TENANT_ID, result.getAccountId());
        assertNull(result.getAccountName());
        assertNull(result.getAccountArn());
        assertNull(result.getEmailAddress());
        assertNull(result.getCanonicalId());
    }

    @Test
    public void  testToCreateUserRequest() {
        final OsisUser osisUser = new OsisUser();
        osisUser.setCanonicalUserId(TEST_USER_ID);
        osisUser.setTenantId(TEST_TENANT_ID);
        osisUser.setActive(true);
        osisUser.setUsername(TEST_NAME);
        osisUser.setCdUserId(TEST_USER_ID);
        osisUser.setRole(OsisUser.RoleEnum.TENANT_USER);
        osisUser.setEmail(SAMPLE_SCALITY_USER_EMAIL);
        osisUser.setCdTenantId(TEST_TENANT_ID);

        final CreateUserRequest createUserRequest =  ScalityModelConverter.toCreateUserRequest(osisUser);

        assertEquals(TEST_USER_ID, createUserRequest.getUserName());
        assertTrue(createUserRequest.getPath().contains(TEST_NAME));
        assertTrue(createUserRequest.getPath().contains(OsisUser.RoleEnum.TENANT_USER.getValue()));
        assertTrue(createUserRequest.getPath().contains(SAMPLE_SCALITY_USER_EMAIL));
        assertTrue(createUserRequest.getPath().contains(TEST_TENANT_ID));
    }

    @Test
    public void testToCreateUserAccessKeyRequest() {
        // Setup
        final CreateAccessKeyRequest expectedResult = new CreateAccessKeyRequest(TEST_USER_ID);

        // Run the test
        final CreateAccessKeyRequest result = ScalityModelConverter.toCreateUserAccessKeyRequest(TEST_USER_ID);

        // Verify the results
        assertEquals(expectedResult, result);
    }

    @Test
    public void testToGetPolicyRequest() {
        // Setup
        final GetPolicyRequest expectedResult = new GetPolicyRequest();
        expectedResult.setPolicyArn("arn:aws:iam::" + TEST_TENANT_ID +":policy/userPolicy@" + TEST_TENANT_ID);

        // Run the test
        final GetPolicyRequest result = ScalityModelConverter.toGetPolicyRequest(TEST_TENANT_ID);

        // Verify the results
        assertEquals(expectedResult, result);
    }

    @Test
    public void testToCreateUserPolicyRequest() {
        // Setup
        final CreatePolicyRequest expectedResult = new CreatePolicyRequest();
        expectedResult.setPolicyName("userPolicy@" + TEST_TENANT_ID);
        expectedResult.setPolicyDocument(DEFAULT_USER_POLICY_DOCUMENT);
        expectedResult.setDescription("This is a common user policy created by OSIS for all the users belonging to the "+ TEST_TENANT_ID +" account");

        // Run the test
        final CreatePolicyRequest result = ScalityModelConverter.toCreateUserPolicyRequest(TEST_TENANT_ID);

        // Verify the results
        assertEquals(expectedResult, result);
    }

    @Test
    public void testToAttachUserPolicyRequest() {
        // Setup
        final AttachUserPolicyRequest expectedResult = new AttachUserPolicyRequest();
        expectedResult.setUserName(TEST_USER_ID);
        expectedResult.setPolicyArn(TEST_POLICY_ARN);

        // Run the test
        final AttachUserPolicyRequest result = ScalityModelConverter.toAttachUserPolicyRequest(TEST_POLICY_ARN, TEST_USER_ID);

        // Verify the results
        assertEquals(expectedResult, result);
    }

    @Test
    public void testToOsisUser() {
        // Setup
        final CreateUserResult createUserResult = new CreateUserResult();
        final String path = "/"+ TEST_NAME +"/"
                + OsisUser.RoleEnum.TENANT_USER.getValue() +"/"
                + SAMPLE_SCALITY_USER_EMAIL  + "/"
                + TEST_TENANT_ID  + "/";
        createUserResult.setUser(new User(path, TEST_USER_ID, TEST_USER_ID, "arn", new GregorianCalendar(2019, Calendar.JANUARY, 1).getTime()));

        final OsisUser osisUser = new OsisUser();
        osisUser.setTenantId(TEST_TENANT_ID);

        // Run the test
        final OsisUser result = ScalityModelConverter.toOsisUser(createUserResult, osisUser.getTenantId());

        // Verify the results
        assertEquals(TEST_USER_ID, result.getCdUserId());
        assertEquals(TEST_USER_ID, result.getUserId());
        assertEquals(TEST_NAME, result.getUsername());
        assertEquals(TEST_TENANT_ID, result.getCdTenantId());
        assertEquals(TEST_TENANT_ID, result.getTenantId());
        assertEquals(OsisUser.RoleEnum.TENANT_USER, result.getRole());
        assertEquals(SAMPLE_SCALITY_USER_EMAIL, result.getEmail());
        assertTrue(result.getActive());
    }

    @Test
    public void testToOsisS3Credentials() {
        // Setup
        final CreateAccessKeyResult createAccessKeyResult = new CreateAccessKeyResult();
        createAccessKeyResult.setAccessKey(new AccessKey(TEST_USER_ID, TEST_ACCESS_KEY, "status", TEST_SECRET_KEY));

        // Run the test
        final OsisS3Credential result = ScalityModelConverter.toOsisS3Credentials(TEST_TENANT_ID, TEST_TENANT_ID, TEST_NAME, createAccessKeyResult);

        // Verify the results
        assertEquals(TEST_USER_ID, result.getCdUserId());
        assertEquals(TEST_USER_ID, result.getUserId());
        assertEquals(TEST_NAME, result.getUsername());
        assertEquals(TEST_TENANT_ID, result.getTenantId());
        assertEquals(TEST_TENANT_ID, result.getCdTenantId());
        assertEquals(TEST_ACCESS_KEY, result.getAccessKey());
        assertEquals(TEST_SECRET_KEY, result.getSecretKey());
    }

    @Test
    public void testToIAMListUsersRequest() {
        // Setup
        // Run the test
        final ListUsersRequest result = ScalityModelConverter.toIAMListUsersRequest(0L, 1000L);

        // Verify the results
        assertEquals(1000, result.getMaxItems());
        assertEquals("0", result.getMarker());
    }

    @Test
    public void testToPageOfUsers() {
        // Setup
        final String path = "/"+ TEST_NAME +"/"
                + OsisUser.RoleEnum.TENANT_USER.getValue() +"/"
                + SAMPLE_SCALITY_USER_EMAIL  + "/"
                + TEST_TENANT_ID  + "/";

        final User user = new User(path, TEST_USER_ID, TEST_USER_ID, "arn", new GregorianCalendar(2019, Calendar.JANUARY, 1).getTime());

        final ListUsersResult listUsersResult = new ListUsersResult().withUsers(Collections.singletonList(user));
        // Run the test
        final PageOfUsers pageOfUsers = ScalityModelConverter.toPageOfUsers(listUsersResult, 0, 1000, SAMPLE_TENANT_ID);

        // Verify the results
        assertTrue(pageOfUsers.getItems().size() > 0);
        assertNotNull(pageOfUsers.getPageInfo());

        final OsisUser osisUser = pageOfUsers.getItems().get(0);

        assertEquals(TEST_USER_ID, osisUser.getUserId());
        assertEquals(SAMPLE_TENANT_ID, osisUser.getTenantId());
        assertEquals(TEST_USER_ID, osisUser.getCdUserId());
        assertEquals(TEST_NAME, osisUser.getUsername());
        assertEquals(SAMPLE_SCALITY_USER_EMAIL, osisUser.getEmail());
        assertEquals(TEST_TENANT_ID, osisUser.getCdTenantId());
        assertEquals(OsisUser.RoleEnum.TENANT_USER, osisUser.getRole());
        assertEquals(TEST_USER_ID, osisUser.getCanonicalUserId());
        assertTrue(osisUser.getActive());
    }

    @Test
    public void testExtractCdTenantIdFilter() {
        // Setup
        final String filter = "cd_tenant_id==" + SAMPLE_CD_TENANT_ID + ";display_name==" + TEST_NAME;

        // Run the test
        final String cdTenantIdFilter = ScalityModelConverter.extractCdTenantIdFilter(filter);

        // Verify the results
        assertEquals("cd_tenant_id==" + SAMPLE_CD_TENANT_ID, cdTenantIdFilter);
    }

    @Test
    public void testExtractOsisUserName() {
        // Setup
        final String filter = "cd_tenant_id==" + SAMPLE_CD_TENANT_ID + ";display_name==" + TEST_NAME;

        // Run the test
        final String osisUserName = ScalityModelConverter.extractOsisUserName(filter);

        // Verify the results
        assertEquals(TEST_NAME, osisUserName);
    }

    @Test
    public void testToGetUserRequest() {
        // Setup

        // Run the test
        final GetUserRequest result = ScalityModelConverter.toIAMGetUserRequest(TEST_USER_ID);

        // Verify the results
        assertEquals(TEST_USER_ID, result.getUserName());
    }

    @Test
    public void testToIAMListAccessKeysRequest() {
        // Setup
        // Run the test
        final ListAccessKeysRequest result = ScalityModelConverter.toIAMListAccessKeysRequest(TEST_USER_ID, 1000L);

        // Verify the results
        assertEquals(1000, result.getMaxItems());
        assertEquals(TEST_USER_ID, result.getUserName());
    }

    @Test
    public void testToPageOfS3Credentials() {
        // Setup

        final AccessKeyMetadata accesskeyMetaData = new AccessKeyMetadata()
                .withAccessKeyId(TEST_ACCESS_KEY)
                .withCreateDate(new Date())
                .withStatus(StatusType.Active)
                .withUserName(TEST_USER_ID);
        final ListAccessKeysResult listAccessKeysResult = new ListAccessKeysResult()
                                                                .withAccessKeyMetadata(Collections.singletonList(accesskeyMetaData));

        final Map<String, String> secretKeyMap = new HashMap<>();
        secretKeyMap.put(TEST_ACCESS_KEY, TEST_SECRET_KEY);
        // Run the test
        final PageOfS3Credentials pageOfS3Credentials = ScalityModelConverter.toPageOfS3Credentials(listAccessKeysResult, 0, 1000, SAMPLE_TENANT_ID, secretKeyMap);

        // Verify the results
        assertTrue(pageOfS3Credentials.getItems().size() > 0);
        assertNotNull(pageOfS3Credentials.getPageInfo());

        final OsisS3Credential result = pageOfS3Credentials.getItems().get(0);

        assertEquals(TEST_USER_ID, result.getCdUserId());
        assertEquals(TEST_USER_ID, result.getUserId());
        assertEquals(SAMPLE_TENANT_ID, result.getTenantId());
        assertEquals(TEST_ACCESS_KEY, result.getAccessKey());
        assertEquals(TEST_SECRET_KEY, result.getSecretKey());
    }

    @Test
    public void testToPageOfS3CredentialsNotAvailableKey() {
        // Setup

        final AccessKeyMetadata accesskeyMetaData = new AccessKeyMetadata()
                .withAccessKeyId(TEST_ACCESS_KEY)
                .withCreateDate(new Date())
                .withStatus(StatusType.Active)
                .withUserName(TEST_USER_ID);
        final AccessKeyMetadata accesskeyMetaData2 = new AccessKeyMetadata()
                .withAccessKeyId(TEST_ACCESS_KEY_2)
                .withCreateDate(new Date())
                .withStatus(StatusType.Active)
                .withUserName(TEST_USER_ID);

        final List<AccessKeyMetadata> accessKeyMetadataList = new ArrayList<>();
        accessKeyMetadataList.add(accesskeyMetaData);
        accessKeyMetadataList.add(accesskeyMetaData2);

        final ListAccessKeysResult listAccessKeysResult = new ListAccessKeysResult()
                .withAccessKeyMetadata(accessKeyMetadataList);

        final Map<String, String> secretKeyMap = new HashMap<>();
        // Secret key only for accesskeyMetaData
        secretKeyMap.put(TEST_ACCESS_KEY, TEST_SECRET_KEY);
        // Run the test
        final PageOfS3Credentials pageOfS3Credentials = ScalityModelConverter.toPageOfS3Credentials(listAccessKeysResult, 0, 1000, SAMPLE_TENANT_ID, secretKeyMap);

        // Verify the results
        assertTrue(pageOfS3Credentials.getItems().size() > 0);
        assertNotNull(pageOfS3Credentials.getPageInfo());

        // First entry always should have secret key
        final OsisS3Credential resultWithSecret = pageOfS3Credentials.getItems().get(0);

        assertEquals(TEST_USER_ID, resultWithSecret.getCdUserId());
        assertEquals(TEST_USER_ID, resultWithSecret.getUserId());
        assertEquals(SAMPLE_TENANT_ID, resultWithSecret.getTenantId());
        assertEquals(TEST_ACCESS_KEY, resultWithSecret.getAccessKey());
        assertEquals(TEST_SECRET_KEY, resultWithSecret.getSecretKey());

        // Last entry should have secret key as "Not Available"
        final OsisS3Credential resultWithNoSecret = pageOfS3Credentials.getItems().get(pageOfS3Credentials.getItems().size()-1);

        assertEquals(TEST_USER_ID, resultWithNoSecret.getCdUserId());
        assertEquals(TEST_USER_ID , resultWithNoSecret.getUserId());
        assertEquals(SAMPLE_TENANT_ID, resultWithNoSecret.getTenantId());
        assertEquals(TEST_ACCESS_KEY_2, resultWithNoSecret.getAccessKey());
        assertEquals(ScalityConstants.NOT_AVAILABLE, resultWithNoSecret.getSecretKey());

    }

    @Test
    public void testMaskSecretKey() {
        // Setup
        final String sampleLog = "{\"items\":[{\"accessKey\":\"MGUWBY4ORDS8RKUQM86P\",\"secretKey\":\"4t3peduUGjO4HYIKJZ54\\u003dGmsfgIP8HCyMS6coVfc\",\"active\":true,\"creationDate\":{\"seconds\":1623302064,\"nanos\":0},\"tenantId\":\"475396941524\",\"userId\":\"99da7ffe-dd82-48a2-b07b-ce200da33005\",\"cdUserId\":\"99da7ffe-dd82-48a2-b07b-ce200da33005\"},{\"accessKey\":\"WWA4UPWFA5EM1MKZE8ZC\",\"secretKey\":\"zjwsRPUA3SUP9aRcVc/+NUO/PPz+F77sVICwCKi\\u003d\",\"active\":true,\"creationDate\":{\"seconds\":1623301987,\"nanos\":0},\"tenantId\":\"475396941524\",\"userId\":\"99da7ffe-dd82-48a2-b07b-ce200da33005\",\"cdUserId\":\"99da7ffe-dd82-48a2-b07b-ce200da33005\"}],\"pageInfo\":{\"limit\":1000,\"offset\":0,\"total\":2}}" ;
        final String maskedLog = "{\"items\":[{\"accessKey\":\"MGUWBY4ORDS8RKUQM86P\",\"secretKey\":\"" + MASKED_SENSITIVE_DATA_STR + "\",\"active\":true,\"creationDate\":{\"seconds\":1623302064,\"nanos\":0},\"tenantId\":\"475396941524\",\"userId\":\"99da7ffe-dd82-48a2-b07b-ce200da33005\",\"cdUserId\":\"99da7ffe-dd82-48a2-b07b-ce200da33005\"},{\"accessKey\":\"WWA4UPWFA5EM1MKZE8ZC\",\"secretKey\":\"" + MASKED_SENSITIVE_DATA_STR + "\",\"active\":true,\"creationDate\":{\"seconds\":1623301987,\"nanos\":0},\"tenantId\":\"475396941524\",\"userId\":\"99da7ffe-dd82-48a2-b07b-ce200da33005\",\"cdUserId\":\"99da7ffe-dd82-48a2-b07b-ce200da33005\"}],\"pageInfo\":{\"limit\":1000,\"offset\":0,\"total\":2}}" ;

        // Run the test
        final String result = ScalityModelConverter.maskSecretKey(sampleLog);

        // Verify the results
        assertEquals(maskedLog, result);
    }
}
