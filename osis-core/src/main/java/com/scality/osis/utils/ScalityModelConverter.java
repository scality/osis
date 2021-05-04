/**
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.utils;

import com.amazonaws.services.identitymanagement.model.AttachRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.CreatePolicyRequest;
import com.amazonaws.services.identitymanagement.model.CreateRoleRequest;
import com.amazonaws.services.identitymanagement.model.DeleteAccessKeyRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.scality.vaultclient.dto.GenerateAccountAccessKeyRequest;
import com.scality.vaultclient.dto.GenerateAccountAccessKeyResponse;
import com.scality.vaultclient.dto.GetAccountRequestDTO;
import com.scality.vaultclient.dto.ListAccountsRequestDTO;
import com.scality.vaultclient.dto.ListAccountsResponseDTO;
import com.vmware.osis.model.OsisTenant;
import com.vmware.osis.model.PageInfo;
import com.vmware.osis.model.PageOfTenants;
import com.vmware.osis.model.exception.BadRequestException;
import com.scality.vaultclient.dto.AccountData;
import com.scality.vaultclient.dto.CreateAccountRequestDTO;
import com.scality.vaultclient.dto.CreateAccountResponseDTO;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.scality.osis.utils.ScalityConstants.*;

/**
 * Convert between OSIS and Scality models
 */
public final class ScalityModelConverter {


    private ScalityModelConverter() {
    }

    /* All Converter methods below converts OSIS model objects to Vault/Scality request formats */

    /**
     * Converts OSIS Tenant object to Vault Create Account request
     * @param osisTenant the osis tenant
     *
     * @return the create account request dto
     */
    public static CreateAccountRequestDTO toScalityCreateAccountRequest(OsisTenant osisTenant) {
        if(!osisTenant.getActive()){
//            Scality does not support inactive tenant creation
            throw new BadRequestException("Creating inactive tenant is not supported");
        }
        CreateAccountRequestDTO createAccountRequestDTO = new CreateAccountRequestDTO();

        if (!StringUtils.isEmpty(osisTenant.getTenantId())) {
            createAccountRequestDTO.setExternalAccountId(osisTenant.getTenantId());
        }
        createAccountRequestDTO.setName(osisTenant.getName());

        createAccountRequestDTO.setEmailAddress(generateTenantEmail(osisTenant.getName()));

        createAccountRequestDTO.setCustomAttributes(toScalityCustomAttributes(osisTenant.getCdTenantIds()));

        return createAccountRequestDTO;
    }

    /**
     * Created Vault List Accounts request for List Tenants
     * @param limit the max number of items
     *
     * @return the create account request dto
     */
    public static ListAccountsRequestDTO toScalityListAccountsRequest(long limit) {
        return ListAccountsRequestDTO.builder()
                .maxItems((int) limit)
                .filterKeyStartsWith(CD_TENANT_ID_PREFIX)
                .build();
    }

    /**
     * Created Vault List Accounts request for Query Tenants
     * @param limit the max number of items
     *
     * @return the create account request dto
     */
    public static ListAccountsRequestDTO toScalityListAccountsRequest(long limit, String filter) {
        return ListAccountsRequestDTO.builder()
                .maxItems((int) limit)
                .filterKey(filter)
                .build();
    }

    /**
     * Creates Vault Assume Role request request for given account id
     * @param accountID the account ID
     *
     * @return the assume role request dto
     */
    public static AssumeRoleRequest getAssumeRoleRequestForAccount(String accountID, String roleName) {
        return new AssumeRoleRequest()
                .withRoleArn(toRoleArn(accountID, roleName))
                .withRoleSessionName(ROLE_SESSION_NAME_PREFIX + new Date().getTime());
    }

    /**
     * Creates GenerateAccountAccessKeyRequest dto for given account id and duration in seconds
     * @param accountID the account ID
     * @param durationSeconds the duration in seconds
     *
     * @return the GenerateAccountAccessKeyRequest dto
     */
    public static GenerateAccountAccessKeyRequest toGenerateAccountAccessKeyRequest(String accountID,
                                                                                    long durationSeconds) {
        return GenerateAccountAccessKeyRequest.builder()
                .accountName(accountID)
                .durationSeconds(durationSeconds)
                .build();
    }

    /**
     * Creates CreateRoleRequest dto
     * @param assumeRoleName the role name
     *
     * @return the CreateRoleRequest dto
     */
    public static CreateRoleRequest toCreateOSISRoleRequest(String assumeRoleName) {
        return new CreateRoleRequest()
                .withRoleName(assumeRoleName)
                .withAssumeRolePolicyDocument(DEFAULT_OSIS_ROLE_INLINE_POLICY);
    }

    /**
     * Creates CreatePolicyRequest dto for admin policy for OSIS role
     * @param tenantId the account id
     *
     * @return the CreatePolicyRequest dto
     */
    public static CreatePolicyRequest toCreateAdminPolicyRequest(String tenantId) {
        return new CreatePolicyRequest()
                .withPolicyName(toAdminPolicyName(tenantId))
                .withPolicyDocument(DEFAULT_ADMIN_POLICY_DOCUMENT)
                .withDescription(toAdminPolicyDescription(tenantId));
    }

    /**
     * Creates AttachRolePolicyRequest dto for attaching admin policy to OSIS role
     * @param policyArn the policy arn
     * @param roleName the OSIS role name
     *
     * @return the AttachRolePolicyRequest dto
     */
    public static AttachRolePolicyRequest toAttachAdminPolicyRequest(String policyArn, String roleName) {
        return new AttachRolePolicyRequest()
                .withPolicyArn(policyArn)
                .withRoleName(roleName);
    }

    /**
     * Creates DeleteAccessKeyRequest dto
     * @param accessKeyId the accesskeyId
     *
     * @return the DeleteAccessKeyRequest dto
     */
    public static DeleteAccessKeyRequest toDeleteAccessKeyRequest(String accessKeyId, String username) {
        DeleteAccessKeyRequest deleteAccessKeyRequest = new DeleteAccessKeyRequest()
                .withAccessKeyId(accessKeyId);

        if(!StringUtils.isEmpty(username)) {
            deleteAccessKeyRequest.setUserName(username);
        }

        return deleteAccessKeyRequest;
    }

    /**
     * Creates GetAccountRequestDTO
     * @param accountID the accountID
     *
     * @return the GetAccountRequestDTO
     */
    public static GetAccountRequestDTO toGetAccountRequestWithID(String accountID) {
        return GetAccountRequestDTO.builder()
                .accountId(accountID)
                .build();
    }

    public static String toAdminPolicyArn(String accountID) {
        return ACCOUNT_ADMIN_POLICY_ARN_REGEX
                .replace(ACCOUNT_ID_REGEX, accountID);
    }

    private static String toAdminPolicyName(String accountID) {
        return ACCOUNT_ADMIN_POLICY_NAME_REGEX
                .replace(ACCOUNT_ID_REGEX, accountID);
    }

    private static String toAdminPolicyDescription(String accountID) {
        return DEFAULT_ADMIN_POLICY_DESCRIPTION.replace(ACCOUNT_ID_REGEX, accountID);
    }

    /**
     * Generates tenant email string using tenant name.
     *  example email address: tenant.name@osis.scality.com
     *
     * @param name the name
     * @return the string
     */
    public static String generateTenantEmail(String name) {
        return name.replaceAll(" ", SEPARATOR) + TENANT_EMAIL_SUFFIX;
    }

    /**
     * Converts OSIS cdTenantIDs list to Vault Account customAttributes Map Format
     *
     * @param cdTenantIds the cd tenant ids list. Example:<code>["9b7e3259-aace-414c-bfd8-94daa0efefaf","7b7e3259-aace-414c-bfd8-94daa0efefaf"]</code>
     * @return the customAttributes map.
     *          Example: <code>{("cd_tenant_id==9b7e3259-aace-414c-bfd8-94daa0efefaf", ""),
     *                          ("cd_tenant_id==7b7e3259-aace-414c-bfd8-94daa0efefaf", "")}</code>
     */
    public static Map<String,String> toScalityCustomAttributes(List<String> cdTenantIds) {
        return cdTenantIds.stream()
                .collect(Collectors.toMap(str-> CD_TENANT_ID_PREFIX + str, str -> ""));
    }

    /**
     * Converts account ID to Vault super role arn
     *
     * @param accountID account id
     * @param roleName role name
     * @return the role arn.
     *          Example: <code>arn:aws:iam::[account-id]:role/[role-name]</code>
     */
    private static String toRoleArn(String accountID, String roleName) {
        return ROLE_ARN_FORMAT
                .replace(ACCOUNT_ID_REGEX, accountID)
                .replace(ROLE_NAME_REGEX, roleName);
    }

    /* Converter methods below will convert Vault/Scality responses to OSIS model objects @param accountResponse the account response */


    /**
     * Converts Vault Create Account response to OSIS Tenant object
     *
     * @return the osis tenant
     */
    public static OsisTenant toOsisTenant(CreateAccountResponseDTO accountResponse) {
        AccountData account = accountResponse.getAccount().getData();
        return toOsisTenant(account);
    }

    /**
     * Converts Vault AccountData object to OSIS Tenant object
     *
     * @return the osis tenant
     */
    private static OsisTenant toOsisTenant(AccountData account) {
        return new OsisTenant()
                .name(account.getName())
                .active(true)
                .cdTenantIds(toOsisCDTenantIds(account.getCustomAttributes()))
                .tenantId(account.getId());
    }

    /**
     * Converts Vault Account customAttributes Map Format to OSIS cdTenantIDs list.
     *
     * @param customAttributes the customAttributes map.
     *      *          Example: <code>{("cd_tenant_id==9b7e3259-aace-414c-bfd8-94daa0efefaf", ""),
     *      *                          ("cd_tenant_id==7b7e3259-aace-414c-bfd8-94daa0efefaf", "")}</code>
     * @return the cd tenant ids list. Example:<code>["9b7e3259-aace-414c-bfd8-94daa0efefaf","7b7e3259-aace-414c-bfd8-94daa0efefaf"]</code>
     */
    public static List<String> toOsisCDTenantIds(Map<String,String> customAttributes) {
        if(customAttributes.size() == 0)
            return new ArrayList<>();

        List<String> cdTenantIds = new ArrayList<String>(customAttributes.keySet()).stream().collect(Collectors.toList());
        cdTenantIds.replaceAll(x -> StringUtils.removeStart(x, CD_TENANT_ID_PREFIX));
        return cdTenantIds;
    }

    /**
     * Converts Vault List Accounts response to OSIS page of tenants
     *
     * @param listAccountsResponseDTO the list accounts response dto
     * @param offset
     * @param limit
     * @return the page of tenants
     */
    public static PageOfTenants toPageOfTenants(ListAccountsResponseDTO listAccountsResponseDTO, long offset, long limit) {
        List<OsisTenant> tenantItems = new ArrayList<>();

        for(AccountData account: listAccountsResponseDTO.getAccounts()){
            tenantItems.add(toOsisTenant(account));
        }

        PageInfo pageInfo = new PageInfo();
        pageInfo.setLimit(limit);
        pageInfo.setOffset(offset);
        pageInfo.setTotal((long) tenantItems.size());

        PageOfTenants pageOfTenants = new PageOfTenants();
        pageOfTenants.items(tenantItems);
        pageOfTenants.setPageInfo(pageInfo);
        return pageOfTenants;
    }

    /**
     * Converts Vault Generate Account AccessKey response to AWS Credentials
     *
     * @param generateAccountAccessKeyResponse the Generate Account AccessKey response dto
     * @return the AWS credentials for API authentication.
     */
    public static Credentials toCredentials(GenerateAccountAccessKeyResponse generateAccountAccessKeyResponse) {
        return new Credentials()
                .withAccessKeyId(generateAccountAccessKeyResponse.getData().getId())
                .withSecretAccessKey(generateAccountAccessKeyResponse.getData().getValue())
                .withExpiration(generateAccountAccessKeyResponse.getData().getNotAfter());
    }
}
