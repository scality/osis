/**
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.utils;

import com.amazonaws.services.identitymanagement.model.User;
import com.amazonaws.services.identitymanagement.model.*;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.amazonaws.services.s3.model.Bucket;
import com.scality.osis.model.*;
import com.scality.osis.model.exception.BadRequestException;
import com.scality.vaultclient.dto.*;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.scality.osis.utils.ScalityConstants.*;

/**
 * Convert between OSIS and Scality models
 */
public final class ScalityModelConverter {

    private ScalityModelConverter() {
    }

    /*
     * All Converter methods below converts OSIS model objects to Vault/Scality
     * request formats
     */

    /**
     * Converts OSIS Tenant object to Vault Create Account request
     *
     * @param osisTenant the osis tenant
     *
     * @return the create account request dto
     */
    public static CreateAccountRequestDTO toScalityCreateAccountRequest(OsisTenant osisTenant) {
        if (!osisTenant.getActive()) {
            // Scality does not support inactive tenant creation
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
     * Converts OSIS Tenant object to Vault Update Account Attributes request
     *
     * @param osisTenant the osis tenant
     *
     * @return the update account attributes request dto
     */
    public static UpdateAccountAttributesRequestDTO toUpdateAccountAttributesRequestDTO(OsisTenant osisTenant) {
        if (!osisTenant.getActive()) {
            // Scality does not support inactive tenants
            throw new BadRequestException("Deactivating a tenant is not supported");
        }
        UpdateAccountAttributesRequestDTO updateAccountAttributesRequestDTO = new UpdateAccountAttributesRequestDTO();

        updateAccountAttributesRequestDTO.setName(osisTenant.getName());

        updateAccountAttributesRequestDTO.setCustomAttributes(toScalityCustomAttributes(osisTenant.getCdTenantIds()));

        return updateAccountAttributesRequestDTO;
    }

    /**
     * Created Vault List Accounts request for List Tenants
     *
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
     * Converts accessKey to Vault Get User by AccessKey request
     *
     * @param accessKey the accessKey
     *
     * @return the Get User by AccessKey request dto
     */
    public static GetUserByAccessKeyRequestDTO toScalityGetUserByAccessKeyRequest(String accessKey) {
        return GetUserByAccessKeyRequestDTO.builder()
                .accessKey(accessKey)
                .build();
    }

    /**
     * Creates Vault List Users request for List users
     *
     * @param limit  the max number of items
     *
     * @param offset the starting offset of the users list
     * @param limit  the max number of the users in the response
     * @return the IAM list users request dto
     */
    public static ListUsersRequest toIAMListUsersRequest(Long offset, Long limit) {
        return new ListUsersRequest()
                .withMarker(offset.toString())
                .withMaxItems(limit.intValue());
    }

    /**
     * Creates Vault List Access Keys request
     *
     * @param username the IAM User's username
     * @param limit    the max number of the access keys in the response
     * @return the IAM list Access Keys request dto
     */
    public static ListAccessKeysRequest toIAMListAccessKeysRequest(String username, Long limit) {
        return new ListAccessKeysRequest()
                .withUserName(username)
                .withMaxItems(limit.intValue());
    }

    /**
     * Creates Vault Update Access Key request
     *
     * @param username the IAM User's username
     * @param accessKey access key
     * @param isActive true/false
     * @return the IAM update Access Keys request dto
     */
    public static UpdateAccessKeyRequest toIAMUpdateAccessKeyRequest(String username, String accessKey,
            Boolean isActive) {
        return new UpdateAccessKeyRequest()
                .withUserName(username)
                .withAccessKeyId(accessKey)
                .withStatus(isActive ? StatusType.Active : StatusType.Inactive);
    }

    /**
     * Creates IAM Get User request
     *
     * @param username Vault username
     *
     * @return the IAM get user request dto
     */
    public static GetUserRequest toIAMGetUserRequest(String username) {
        return new GetUserRequest()
                .withUserName(username);
    }

    /**
     * Creates IAM Delete User request
     *
     * @param username Vault username
     *
     * @return the IAM delete user request dto
     */
    public static DeleteUserRequest toIAMDeleteUserRequest(String username) {
        return new DeleteUserRequest()
                .withUserName(username);
    }

    /**
     * Created Vault List Accounts request for Query Tenants
     *
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
     *
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
     * Creates GenerateAccountAccessKeyRequest dto for given account id and duration
     * in seconds
     *
     * @param accountID       the account ID
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
     *
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
     *
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
     *
     * @param policyArn the policy arn
     * @param roleName  the OSIS role name
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
     *
     * @param accessKeyId the accesskeyId
     * @param username    the username
     *
     * @return the DeleteAccessKeyRequest dto
     */
    public static DeleteAccessKeyRequest toDeleteAccessKeyRequest(String accessKeyId, String username) {
        DeleteAccessKeyRequest deleteAccessKeyRequest = new DeleteAccessKeyRequest()
                .withAccessKeyId(accessKeyId);

        if (!StringUtils.isEmpty(username)) {
            deleteAccessKeyRequest.setUserName(username);
        }

        return deleteAccessKeyRequest;
    }

    /**
     * Creates GetAccountRequestDTO
     *
     * @param accountID the accountID
     *
     * @return the GetAccountRequestDTO
     */
    public static GetAccountRequestDTO toGetAccountRequestWithID(String accountID) {
        return GetAccountRequestDTO.builder()
                .accountId(accountID)
                .build();
    }

    /**
     * Creates GetAccountRequestDTO using canonicalID
     *
     * @param canonicalID the canonicalID
     *
     * @return the GetAccountRequestDTO
     */
    public static GetAccountRequestDTO toGetAccountRequestWithCanonicalID(String canonicalID) {
        return GetAccountRequestDTO.builder()
                .canonicalId(canonicalID)
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

    public static CreateUserRequest toCreateUserRequest(OsisUser osisUser) {
        OsisUser.RoleEnum role = (osisUser.getRole() != null) ? osisUser.getRole() : OsisUser.RoleEnum.UNKNOWN;
        return new CreateUserRequest()
                .withUserName(osisUser.getCdUserId())
                .withPath(
                        toUserPath(osisUser.getUsername(),
                                role,
                                osisUser.getEmail(),
                                osisUser.getCdTenantId(),
                                osisUser.getCanonicalUserId()));
    }

    private static String toUserPath(String username, OsisUser.RoleEnum role, String email, String cdTenantId,
            String canonicalUserId) {
        return USER_PATH_SEPARATOR + username + USER_PATH_SEPARATOR + role.getValue() + USER_PATH_SEPARATOR + email
                + USER_PATH_SEPARATOR + cdTenantId + USER_PATH_SEPARATOR
                + canonicalUserId + USER_PATH_SEPARATOR;
    }

    public static CreateAccessKeyRequest toCreateUserAccessKeyRequest(String userID) {
        return new CreateAccessKeyRequest(userID);
    }

    public static GetPolicyRequest toGetPolicyRequest(String accountId) {
        return new GetPolicyRequest()
                .withPolicyArn(toUserPolicyArn(accountId));
    }

    public static GetPolicyRequest toGetAdminPolicyRequest(String accountId) {
        return new GetPolicyRequest()
                .withPolicyArn(toAdminPolicyArn(accountId));
    }

    public static GetPolicyVersionRequest toGetAdminPolicyVersionRequest(String accountId, String versionId) {
        return new GetPolicyVersionRequest()
                .withPolicyArn(toAdminPolicyArn(accountId))
                .withVersionId(versionId);
    }

    public static CreatePolicyRequest toCreateUserPolicyRequest(String tenantId) {
        return new CreatePolicyRequest()
                .withPolicyName(toUserPolicyName(tenantId))
                .withPolicyDocument(DEFAULT_USER_POLICY_DOCUMENT)
                .withDescription(toUserPolicyDescription(tenantId));
    }

    public static CreatePolicyVersionRequest toCreateAdminPolicyVersionRequest(String tenantId) {
        return new CreatePolicyVersionRequest()
                .withPolicyArn(toAdminPolicyArn(tenantId))
                .withPolicyDocument(DEFAULT_ADMIN_POLICY_DOCUMENT)
                .withSetAsDefault(true);
    }
    public static AttachUserPolicyRequest toAttachUserPolicyRequest(String policyArn, String username) {
        return new AttachUserPolicyRequest()
                .withPolicyArn(policyArn)
                .withUserName(username);
    }

    public static DetachUserPolicyRequest toDetachUserPolicyRequest(String policyArn, String username) {
        return new DetachUserPolicyRequest()
                .withPolicyArn(policyArn)
                .withUserName(username);
    }

    private static String toUserPolicyArn(String accountID) {
        return USER_POLICY_ARN_REGEX
                .replace(ACCOUNT_ID_REGEX, accountID);
    }

    private static String toUserPolicyName(String accountID) {
        return USER_POLICY_NAME_REGEX
                .replace(ACCOUNT_ID_REGEX, accountID);
    }

    private static String toUserPolicyDescription(String accountID) {
        return DEFAULT_USER_POLICY_DESCRIPTION.replace(ACCOUNT_ID_REGEX, accountID);
    }

    public static String toRepoKeyForCredentials(String userId, String accessKeyId) {
        return userId + REPO_KEY_SEPARATOR + accessKeyId;
    }

    public static String toRedisHashName(String osisRedisHashKey) {
        return DEFAULT_REDIS_PREFIX + osisRedisHashKey;
    }

    /**
     * Generates tenant email string using tenant name.
     * example email address: tenant.name@osis.scality.com
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
     * @param cdTenantIds the cd tenant ids list.
     *                    Example:<code>["9b7e3259-aace-414c-bfd8-94daa0efefaf","7b7e3259-aace-414c-bfd8-94daa0efefaf"]</code>
     * @return the customAttributes map.
     *         Example:
     *         <code>{("cd_tenant_id==9b7e3259-aace-414c-bfd8-94daa0efefaf", ""),
     *                          ("cd_tenant_id==7b7e3259-aace-414c-bfd8-94daa0efefaf", "")}</code>
     */
    public static Map<String, String> toScalityCustomAttributes(List<String> cdTenantIds) {
        return cdTenantIds.stream()
                .collect(Collectors.toMap(str -> CD_TENANT_ID_PREFIX + str, str -> ""));
    }

    /**
     * Converts account ID to Vault super role arn
     *
     * @param accountID account id
     * @param roleName  role name
     * @return the role arn.
     *         Example: <code>arn:aws:iam::[account-id]:role/[role-name]</code>
     */
    private static String toRoleArn(String accountID, String roleName) {
        return ROLE_ARN_FORMAT
                .replace(ACCOUNT_ID_REGEX, accountID)
                .replace(ROLE_NAME_REGEX, roleName);
    }

    public static String extractCdTenantId(String filter) {
        return filter.split(FILTER_KEY_VALUE_SEPARATOR)[1];
    }

    /*
     * Converter methods below will convert Vault/Scality responses to OSIS model
     * objects @param accountResponse the account response
     */

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
     * @param account account object
     *
     * @return the osis tenant
     */
    public static OsisTenant toOsisTenant(AccountData account) {
        return new OsisTenant()
                .name(account.getName())
                .active(true)
                .cdTenantIds(toOsisCDTenantIds(account.getCustomAttributes()))
                .tenantId(account.getId());
    }

    /**
     * Converts IAM User object to OSIS User object
     *
     * @return the osis User
     */
    public static OsisUser toOsisUser(User user, String tenantId) {
        return new OsisUser()
                .cdUserId(user.getUserName())
                .canonicalUserId(canonicalIDFromUserPath(user.getPath(), user.getUserName()))
                .userId(user.getUserName())
                .active(Boolean.TRUE)
                .cdTenantId(cdTenantIDFromUserPath(user.getPath()))
                .tenantId(tenantId)
                .displayName(nameFromUserPath(user.getPath()))
                .role(roleFromUserPath(user.getPath()))
                .email(emailFromUserPath(user.getPath()));
    }

    /**
     * Converts IAM User object to OSIS User object for get user with canonical id
     *
     * @return the osis User
     */
    public static OsisUser toCanonicalOsisUser(AccountData account, List<OsisUser> users) {
        final OsisUser osisUser = new OsisUser();

        osisUser.setTenantId(account.getId());
        if (!account.getCustomAttributes().isEmpty()) {
            osisUser.setCdTenantId(ScalityModelConverter.toOsisCDTenantIds(account.getCustomAttributes()).get(0));
        }
        osisUser.setCanonicalUserId(account.getCanonicalId());

        if (!users.isEmpty()) {
            OsisUser user = users.get(users.size() - 1);
            osisUser.setUsername(user.getUsername());
            osisUser.setUserId(user.getUserId());
            osisUser.setCdUserId(user.getCdUserId());
            osisUser.setEmail(user.getEmail());
        }
        osisUser.setRole(OsisUser.RoleEnum.TENANT_ADMIN);
        osisUser.setActive(Boolean.TRUE);

        return osisUser;
    }

    /**
     * Converts Vault Account customAttributes Map Format to OSIS cdTenantIDs list.
     *
     * @param customAttributes the customAttributes map.
     *                         * Example:
     *                         <code>{("cd_tenant_id==9b7e3259-aace-414c-bfd8-94daa0efefaf", ""),
     *      *                          ("cd_tenant_id==7b7e3259-aace-414c-bfd8-94daa0efefaf", "")}</code>
     * @return the cd tenant ids list.
     *         Example:<code>["9b7e3259-aace-414c-bfd8-94daa0efefaf","7b7e3259-aace-414c-bfd8-94daa0efefaf"]</code>
     */
    public static List<String> toOsisCDTenantIds(Map<String, String> customAttributes) {
        if (customAttributes == null || customAttributes.size() == 0)
            return new ArrayList<>();

        List<String> cdTenantIds = new ArrayList<String>(customAttributes.keySet()).stream()
                .collect(Collectors.toList());
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
    public static PageOfTenants toPageOfTenants(ListAccountsResponseDTO listAccountsResponseDTO, long offset,
            long limit) {
        List<OsisTenant> tenantItems = new ArrayList<>();

        for (AccountData account : listAccountsResponseDTO.getAccounts()) {
            tenantItems.add(toOsisTenant(account));
        }

        PageInfo pageInfo = new PageInfo(limit, offset, (long) tenantItems.size());

        PageOfTenants pageOfTenants = new PageOfTenants();
        pageOfTenants.items(tenantItems);
        pageOfTenants.setPageInfo(pageInfo);
        return pageOfTenants;
    }

    /**
     * Converts Vault List Accounts response to OSIS page of tenants
     *
     * @param accountData the account data
     * @return the page of tenants
     */
    public static PageOfTenants toPageOfTenants(AccountData accountData, long offset, long limit) {
        List<OsisTenant> tenantItems = Collections.singletonList(toOsisTenant(accountData));

        PageInfo pageInfo = new PageInfo(limit, offset, (long) tenantItems.size());

        PageOfTenants pageOfTenants = new PageOfTenants();
        pageOfTenants.items(tenantItems);
        pageOfTenants.setPageInfo(pageInfo);
        return pageOfTenants;
    }

    /**
     * Converts Vault Generate Account AccessKey response to AWS Credentials
     *
     * @param generateAccountAccessKeyResponse the Generate Account AccessKey
     *                                         response dto
     * @return the AWS credentials for API authentication.
     */
    public static Credentials toCredentials(GenerateAccountAccessKeyResponse generateAccountAccessKeyResponse) {
        return new Credentials()
                .withAccessKeyId(generateAccountAccessKeyResponse.getData().getId())
                .withSecretAccessKey(generateAccountAccessKeyResponse.getData().getValue())
                .withExpiration(generateAccountAccessKeyResponse.getData().getNotAfter());
    }

    public static OsisUser toOsisUser(CreateUserResult createUserResult, String tenantId) {
        User vaultUser = createUserResult.getUser();
        return new OsisUser()
                .cdUserId(vaultUser.getUserName())
                .canonicalUserId(canonicalIDFromUserPath(vaultUser.getPath(), vaultUser.getUserName()))
                .userId(vaultUser.getUserName())
                .active(Boolean.TRUE)
                .cdTenantId(cdTenantIDFromUserPath(vaultUser.getPath()))
                .tenantId(tenantId)
                .displayName(nameFromUserPath(vaultUser.getPath()))
                .role(roleFromUserPath(vaultUser.getPath()))
                .email(emailFromUserPath(vaultUser.getPath()));
    }

    private static String nameFromUserPath(String path) {
        return path.split("/").length > 1 ? path.split("/")[1] : NON_OSIS_USR;
    }

    private static OsisUser.RoleEnum roleFromUserPath(String path) {
        return path.split("/").length > 2 ? OsisUser.RoleEnum.fromValue(path.split("/")[2])
                : OsisUser.RoleEnum.ANONYMOUS;
    }

    private static String emailFromUserPath(String path) {
        return path.split("/").length > 3 ? path.split("/")[3] : "";
    }

    private static String cdTenantIDFromUserPath(String path) {
        return path.split("/").length > 4 ? path.split("/")[4] : "";
    }

    private static String canonicalIDFromUserPath(String path, String userId) {
        return path.split("/").length > 5 ? path.split("/")[5] : userId;
    }

    public static OsisS3Credential toOsisS3Credentials(String cdTenantId, String tenantId, String username,
            CreateAccessKeyResult createAccessKeyResult) {
        return new OsisS3Credential()
                .accessKey(createAccessKeyResult.getAccessKey().getAccessKeyId())
                .secretKey(createAccessKeyResult.getAccessKey().getSecretAccessKey())
                .active(Boolean.TRUE)
                .userId(createAccessKeyResult.getAccessKey().getUserName())
                .cdUserId(createAccessKeyResult.getAccessKey().getUserName())
                .tenantId(tenantId)
                .cdTenantId(cdTenantId)
                .username(username)
                .creationDate(Instant.now())
                .immutable(Boolean.TRUE);
    }

    public static OsisS3Credential toOsisS3Credentials(String tenantId,
                                                       String cdTenantId,
                                                       AccessKeyMetadata accessKeyMetadata,
                                                       String secretKey) {
        OsisS3Credential s3Credential = new OsisS3Credential()
                .accessKey(accessKeyMetadata.getAccessKeyId())
                .active(accessKeyMetadata.getStatus()
                        .equalsIgnoreCase(StatusType.Active.toString()))
                .userId(accessKeyMetadata.getUserName())
                .cdUserId(accessKeyMetadata.getUserName())
                .tenantId(tenantId)
                .cdTenantId(cdTenantId)
                .creationDate(accessKeyMetadata.getCreateDate().toInstant())
                .immutable(Boolean.TRUE)
                .secretKey(StringUtils.isEmpty(secretKey) ? ScalityConstants.NOT_AVAILABLE : secretKey);

        return s3Credential;
    }

    /**
     * Converts IAM List users response to OSIS page of users
     *
     * @param listUsersResult the list users response dto
     * @param offset
     * @param limit
     * @return the page of users
     */
    public static PageOfUsers toPageOfUsers(ListUsersResult listUsersResult, long offset, long limit, String tenantId) {
        List<OsisUser> userItems = new ArrayList<>();

        for (User user : listUsersResult.getUsers()) {
            userItems.add(toOsisUser(user, tenantId));
        }

        PageInfo pageInfo = new PageInfo(limit, offset, (long) userItems.size());

        PageOfUsers pageOfUsers = new PageOfUsers();
        pageOfUsers.items(userItems);
        pageOfUsers.setPageInfo(pageInfo);
        return pageOfUsers;
    }

    /**
     * Converts IAM get user response to OSIS page of users
     *
     * @param osisUser the osis users dto
     * @param offset
     * @param limit
     * @return the page of users
     */
    public static PageOfUsers toPageOfUsers(OsisUser osisUser, long offset, long limit) {

        PageInfo pageInfo = new PageInfo(limit, offset, 1L);

        PageOfUsers pageOfUsers = new PageOfUsers();
        pageOfUsers.items(Collections.singletonList(osisUser));
        pageOfUsers.setPageInfo(pageInfo);
        return pageOfUsers;
    }

    /**
     * Converts IAM List access keys response to OSIS S3 Credentials
     *
     * @param listAccessKeysResult the list access keys response dto
     * @param offset
     * @param limit
     * @param secretKeyMap
     * @return the page of users
     */
    public static PageOfS3Credentials toPageOfS3Credentials(ListAccessKeysResult listAccessKeysResult, long offset,
            long limit, OsisTenant tenant, Map<String, String> secretKeyMap) {
        List<OsisS3Credential> credentials = new ArrayList<>();
        List<OsisS3Credential> credentialsNoSK = new ArrayList<>();

        for (AccessKeyMetadata accessKeyMetadata : listAccessKeysResult.getAccessKeyMetadata()) {

            OsisS3Credential s3Credential = new OsisS3Credential()
                    .accessKey(accessKeyMetadata.getAccessKeyId())
                    .active(accessKeyMetadata.getStatus()
                            .equalsIgnoreCase(StatusType.Active.toString()))
                    .userId(accessKeyMetadata.getUserName())
                    .cdUserId(accessKeyMetadata.getUserName())
                    .tenantId(tenant.getTenantId())
                    .cdTenantId(tenant.getCdTenantIds().get(0))
                    .creationDate(accessKeyMetadata.getCreateDate().toInstant())
                    .immutable(Boolean.TRUE);
            if (null != secretKeyMap.get(accessKeyMetadata.getAccessKeyId())) {
                // If secret key is available, add credential object to the list
                s3Credential.setSecretKey(secretKeyMap.get(accessKeyMetadata.getAccessKeyId()));
                credentials.add(s3Credential);
            } else {
                s3Credential.setSecretKey(ScalityConstants.NOT_AVAILABLE);
                credentialsNoSK.add(s3Credential);
            }
        }

        // Add credential objects with no secret keys to the bottom of the list
        if (!credentialsNoSK.isEmpty()) {
            credentials.addAll(credentialsNoSK);
        }

        PageInfo pageInfo = new PageInfo(limit, offset, (long) credentials.size());

        PageOfS3Credentials pageOfS3Credentials = new PageOfS3Credentials();
        pageOfS3Credentials.items(credentials);
        pageOfS3Credentials.setPageInfo(pageInfo);
        return pageOfS3Credentials;
    }

    /**
     * Empty Page of OSIS S3 Credentials
     *
     * @param offset
     * @param limit
     * @return the page of credentials
     */
    public static PageOfS3Credentials getEmptyPageOfS3Credentials(long offset, long limit) {
        return toPageOfS3Credentials(null, null, offset, limit);
    }

    /**
     * Converts IAM List access keys response to OSIS S3 Credentials
     *
     * @param osisS3Credential the s3credential
     * @param cdTenantId
     * @param offset
     * @param limit
     * @return the page of s3credentials
     */
    public static PageOfS3Credentials toPageOfS3Credentials(OsisS3Credential osisS3Credential, String cdTenantId,
            long offset, long limit) {

        if (osisS3Credential != null) {
            osisS3Credential.setCdTenantId(cdTenantId);
        }

        PageInfo pageInfo = new PageInfo(limit, offset, (osisS3Credential == null) ? 0 : 1L);

        PageOfS3Credentials pageOfS3Credentials = new PageOfS3Credentials();
        pageOfS3Credentials
                .items((osisS3Credential == null) ? new ArrayList<>() : Collections.singletonList(osisS3Credential));
        pageOfS3Credentials.setPageInfo(pageInfo);
        return pageOfS3Credentials;
    }

    public static AccessKeyMetadata toAccessKeyMetadata(AccessKey accessKey) {
        AccessKeyMetadata accessKeyMetadata = new AccessKeyMetadata()
                .withAccessKeyId(accessKey.getAccessKeyId())
                .withUserName(accessKey.getUserName())
                .withStatus(accessKey.getStatus())
                .withCreateDate(accessKey.getCreateDate() != null ? accessKey.getCreateDate() : new Date());
        return accessKeyMetadata;
    }

    public static String maskSecretKey(String logStatement) {
        return logStatement.replaceAll(SECRET_KEY_REGEX, "$1" + MASKED_SENSITIVE_DATA_STR);
    }

    /**
     * Converts S3 Bucket to OSIS bucket meta
     *
     * @param bucket S3 Bucket instance
     * @return the OSIS bucket meta
     */
    public static OsisBucketMeta toOsisBucketMeta(Bucket bucket, String userId) {

        return new OsisBucketMeta()
                .name(bucket.getName())
                .creationDate(bucket.getCreationDate().toInstant())
                .userId(userId);
    }

    /**
     * Converts S3 list buckets response to OSIS page of bucket meta
     *
     * @param buckets the bucket list response
     * @param userId userId
     * @param offset offset
     * @param limit limit
     * @return the OSIS page of bucket meta
     */
    public static PageOfOsisBucketMeta toPageOfOsisBucketMeta(List<Bucket> buckets, String userId, long offset, long limit) {
        List<OsisBucketMeta> bucketItems = new ArrayList<>();

        List<Bucket> selectedBuckets = buckets.stream().skip(offset).limit(limit).collect(Collectors.toList());

        for (Bucket bucket : selectedBuckets) {
            bucketItems.add(toOsisBucketMeta(bucket, userId));
        }

        PageInfo pageInfo = new PageInfo(limit, offset, (long) buckets.size());

        PageOfOsisBucketMeta pageOfOsisBucketMeta = new PageOfOsisBucketMeta();
        pageOfOsisBucketMeta.setItems(bucketItems);
        pageOfOsisBucketMeta.setPageInfo(pageInfo);
        return pageOfOsisBucketMeta;
    }

}
