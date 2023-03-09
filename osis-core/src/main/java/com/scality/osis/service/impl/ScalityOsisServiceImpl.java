/**
 * Copyright 2020 VMware, Inc.
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.service.impl;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.scality.osis.ScalityAppEnv;
import com.scality.osis.model.*;
import com.scality.osis.model.exception.NotFoundException;
import com.scality.osis.model.exception.NotImplementedException;
import com.scality.osis.redis.service.IRedisRepository;
import com.scality.osis.resource.ScalityOsisCapsManager;
import com.scality.osis.s3.S3;
import com.scality.osis.security.crypto.model.SecretKeyRepoData;
import com.scality.osis.security.utils.CipherFactory;
import com.scality.osis.service.ScalityOsisService;
import com.scality.osis.utils.ScalityModelConverter;
import com.scality.osis.utils.ScalityUtils;
import com.scality.osis.vaultadmin.VaultAdmin;
import com.scality.osis.vaultadmin.impl.VaultServiceException;
import com.scality.vaultclient.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.scality.osis.utils.ScalityConstants.*;

/**
 * The type Scality osis service.
 */
@Service
@Primary
public class ScalityOsisServiceImpl implements ScalityOsisService {
    private static final Logger logger = LoggerFactory.getLogger(ScalityOsisServiceImpl.class);

    private ScalityAppEnv appEnv;
    private VaultAdmin vaultAdmin;
    private S3 s3;
    private ScalityOsisCapsManager scalityOsisCapsManager;

    @Autowired
    private AsyncScalityOsisService asyncScalityOsisService;

    @Autowired
    private IRedisRepository<SecretKeyRepoData> scalityRedisRepository;

    @Autowired
    private CipherFactory cipherFactory;

    private final Map<String, SecretKeyRepoData> springLocalCache = new ConcurrentHashMap<>();

    /**
     * Instantiates a new Scality osis service.
     *
     * @param appEnv                 the app env
     * @param vaultAdmin             the vault admin
     * @param s3                     the s3 client
     * @param scalityOsisCapsManager the osis caps manager
     */
    public ScalityOsisServiceImpl(ScalityAppEnv appEnv, VaultAdmin vaultAdmin, S3 s3,
            ScalityOsisCapsManager scalityOsisCapsManager) {
        this.appEnv = appEnv;
        this.vaultAdmin = vaultAdmin;
        this.s3 = s3;
        this.scalityOsisCapsManager = scalityOsisCapsManager;
    }

    /**
     * Create a tenant in the platform
     *
     * @param osisTenant Tenant to create in the platform (required)
     * @return A tenant is created
     */
    @Override
    public OsisTenant createTenant(OsisTenant osisTenant) {
        try {
            logger.info("Create Tenant request received:{}", new Gson().toJson(osisTenant));
            CreateAccountRequestDTO accountRequest = ScalityModelConverter.toScalityCreateAccountRequest(osisTenant);

            logger.debug("[Vault]CreateAccount request:{}", new Gson().toJson(accountRequest));

            CreateAccountResponseDTO accountResponse = vaultAdmin.createAccount(accountRequest);

            logger.debug("[Vault]CreateAccount response:{}", new Gson().toJson(accountResponse));

            OsisTenant resOsisTenant = ScalityModelConverter.toOsisTenant(accountResponse);

            // call async service to setup the assume role for the new tenant
            asyncScalityOsisService.setupAssumeRole(resOsisTenant);

            logger.info("Create Tenant response:{}", new Gson().toJson(resOsisTenant));

            return resOsisTenant;
        } catch (VaultServiceException e) {
            // Create Tenant supports only 400:BAD_REQUEST error, change status code in the
            // VaultServiceException
            logger.error("Create Tenant error. Error details: ", e);
            throw new VaultServiceException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @Override
    public PageOfTenants queryTenants(long offset, long limit, String filter) {
        if (filter.contains(CD_TENANT_ID_PREFIX)) {
            try {
                logger.info("Query Tenants request received: offset={}, limit={}, filter ={}", offset, limit, filter);

                String cdTenantId = ScalityModelConverter.extractCdTenantId(filter);

                PageOfTenants pageOfTenants = null;
                if (ScalityUtils.isValidUUID(cdTenantId)) {

                    ListAccountsRequestDTO listAccountsRequest = ScalityModelConverter
                            .toScalityListAccountsRequest(limit, filter);

                    logger.debug("[Vault] List Accounts Request:{}", new Gson().toJson(listAccountsRequest));
                    ListAccountsResponseDTO listAccountsResponseDTO = vaultAdmin.listAccounts(offset,
                            listAccountsRequest);

                    logger.debug("[Vault] List Accounts response:{}", new Gson().toJson(listAccountsResponseDTO));

                    pageOfTenants = ScalityModelConverter.toPageOfTenants(listAccountsResponseDTO, offset, limit);
                } else {
                    GetAccountRequestDTO getAccountRequest = ScalityModelConverter
                            .toGetAccountRequestWithID(cdTenantId);

                    logger.debug("[Vault] Get Account Request:{}", new Gson().toJson(getAccountRequest));

                    AccountData account = vaultAdmin.getAccount(getAccountRequest);
                    pageOfTenants = ScalityModelConverter.toPageOfTenants(account, offset, limit);
                }

                logger.info("Query Tenants response:{}", new Gson().toJson(pageOfTenants));

                return pageOfTenants;

            } catch (VaultServiceException e) {
                logger.error("Query Tenants error. Return empty list. Error details: ", e);
                // For errors, List Tenants should return empty PageOfTenants
                PageInfo pageInfo = new PageInfo(limit, offset);

                PageOfTenants pageOfTenants = new PageOfTenants();
                pageOfTenants.setItems(new ArrayList<>());
                pageOfTenants.setPageInfo(pageInfo);
                return pageOfTenants;
            }
        } else {
            // Returning all the tenants with given offset and limit as filter is not with
            // `cd_tenant_id`
            return listTenants(offset, limit);
        }
    }

    @Override
    public PageOfTenants listTenants(long offset, long limit) {
        try {
            logger.info("List Tenants request received: offset={}, limit={}", offset, limit);
            ListAccountsRequestDTO listAccountsRequest = ScalityModelConverter.toScalityListAccountsRequest(limit);

            logger.debug("[Vault] List Accounts Request:{}", new Gson().toJson(listAccountsRequest));
            ListAccountsResponseDTO listAccountsResponseDTO = vaultAdmin.listAccounts(offset, listAccountsRequest);

            logger.debug("[Vault] List Accounts response:{}", new Gson().toJson(listAccountsResponseDTO));

            PageOfTenants pageOfTenants = ScalityModelConverter.toPageOfTenants(listAccountsResponseDTO, offset, limit);

            logger.info("List Tenants response:{}", new Gson().toJson(pageOfTenants));

            return pageOfTenants;

        } catch (VaultServiceException e) {
            logger.error("List Tenants error. Return empty list. Error details: ", e);
            // For errors, List Tenants should return empty PageOfTenants
            PageInfo pageInfo = new PageInfo(limit, offset);

            PageOfTenants pageOfTenants = new PageOfTenants();
            pageOfTenants.setItems(new ArrayList<>());
            pageOfTenants.setPageInfo(pageInfo);
            return pageOfTenants;
        }
    }

    @Override
    public OsisUser createUser(OsisUser osisUser) {
        try {
            logger.info("Create User request received:{}", new Gson().toJson(osisUser));

            AccountData accountData = vaultAdmin
                    .getAccount(ScalityModelConverter.toGetAccountRequestWithID(osisUser.getTenantId()));
            osisUser.setCanonicalUserId(accountData.getCanonicalId());

            Credentials tempCredentials = getCredentials(osisUser.getTenantId());
            final AmazonIdentityManagement iam = vaultAdmin.getIAMClient(tempCredentials,
                    appEnv.getRegionInfo().get(0));

            CreateUserRequest createUserRequest = ScalityModelConverter.toCreateUserRequest(osisUser);
            logger.debug("[Vault] Create User Request:{}", new Gson().toJson(createUserRequest));

            CreateUserResult createUserResult = iam.createUser(createUserRequest);

            logger.debug("[Vault] Create User response:{}", new Gson().toJson(createUserResult));

            OsisUser resOsisUser = null;

            if (null != createUserResult) {

                resOsisUser = ScalityModelConverter.toOsisUser(createUserResult, osisUser.getTenantId());

                /** Get userpolicy@<Account_id> **/
                Policy userPolicy = getOrCreateUserPolicy(iam, resOsisUser.getTenantId());

                /** Attach user policy to the user **/
                AttachUserPolicyRequest attachUserPolicyRequest = ScalityModelConverter
                        .toAttachUserPolicyRequest(userPolicy.getArn(), resOsisUser.getUserId());
                logger.debug("[Vault] Attach User Policy Request:{}", new Gson().toJson(attachUserPolicyRequest));

                AttachUserPolicyResult attachUserPolicyResult = iam.attachUserPolicy(attachUserPolicyRequest);
                logger.debug("[Vault] Attach User Policy response:{}", new Gson().toJson(attachUserPolicyResult));

                /** Create User Access Key for the user **/
                OsisS3Credential osisCredential = createOsisCredential(
                        resOsisUser.getTenantId(),
                        resOsisUser.getUserId(),
                        resOsisUser.getCdTenantId(),
                        resOsisUser.getUsername(),
                        iam);

                resOsisUser.setOsisS3Credentials(Arrays.asList(osisCredential));

                logger.info("Create User response:{}",
                        ScalityModelConverter.maskSecretKey(new Gson().toJson(resOsisUser)));

            }

            return resOsisUser;

        } catch (Exception e) {
            if (isAdminPolicyError(e) && !StringUtils.isNullOrEmpty(osisUser.getTenantId())) {
                try {
                    generateAdminPolicy(osisUser.getTenantId());
                    return createUser(osisUser);
                } catch (Exception ex) {
                    e = ex;
                }
            }
            // Create User supports only 400:BAD_REQUEST error, change status code in the
            // VaultServiceException
            logger.error("Create User error. Error details: ", e);
            throw new VaultServiceException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }

    }

    @Override
    public PageOfUsers queryUsers(long offset, long limit, String filter) {
        if (filter.contains(CD_TENANT_ID_PREFIX) &&
                (filter.contains(DISPLAY_NAME_PREFIX)
                        || filter.contains(USERNAME_PREFIX)
                        || filter.contains(CD_USER_ID_PREFIX)
                        || filter.contains(USER_ID_PREFIX))) {
            String tenantId = null;
            try {
                logger.info("Query Users request received:: filter:{}, offset:{}, limit:{}", filter, offset, limit);

                Map<String, String> kvMap = ScalityUtils.parseFilter(filter);
                tenantId = kvMap.get(OSIS_TENANT_ID);
                String userId = kvMap.get(OSIS_USER_ID);
                String cdUserId = kvMap.get(CD_USER_ID);
                String cdTenantId = kvMap.get(CD_TENANT_ID);
                String displayName = kvMap.get(DISPLAY_NAME);
                String username = kvMap.get(USERNAME);

                if (tenantId == null) {
                    // check the format of received cd_tenant_id
                    // 1. format UUID ex.0a0e9c1a-1c27-4908-8d1b-74f87325b47b, represent cd_tenant_id of a tenant
                    if (ScalityUtils.isValidUUID(cdTenantId)) {
                        String cdTenantIdFilter = CD_TENANT_ID_PREFIX + cdTenantId;
                        ListAccountsRequestDTO queryAccountsRequest = ScalityModelConverter
                                .toScalityListAccountsRequest(limit, cdTenantIdFilter);

                        tenantId = vaultAdmin.getAccountID(queryAccountsRequest);
                    } else {
                        // 2. format string of 12 letters, ex. 971317116260, represent tenant_id of a tenant
                        tenantId = cdTenantId;
                    }
                }

                // get the account by TenantId from vault and convert it to OsisTenant
                GetAccountRequestDTO getAccountRequestDTO = new GetAccountRequestDTO();
                getAccountRequestDTO.setAccountId(tenantId);
                logger.debug("[Vault]GetAccount request:{}", new Gson().toJson(getAccountRequestDTO));
                AccountData accountData = vaultAdmin.getAccount(getAccountRequestDTO);
                logger.debug("[Vault]GetAccount response:{}", new Gson().toJson(accountData));
                OsisTenant osisTenant = ScalityModelConverter.toOsisTenant(accountData);
                logger.info("Query Users of tenant {}:", new Gson().toJson(osisTenant));

                // check the format of received display_name
                // 1. format UUID ex.9db66358-a7d2-4fd6-9688-f483e492bdbd, represents user_id of a tenant
                if (ScalityUtils.isValidUUID(displayName)) {
                    logger.debug("Query Users filter display_name represents cd_tenant_id");
                    userId = userId != null ? userId : displayName;
                } else {
                    // 2. format tenant name ex. tenant1, represent name of a tenant
                    if (!Objects.equals(osisTenant.getName(), displayName)) {
                        // 3. format user name ex. user1, represent username of a user
                        logger.debug("Query Users filter display_name represents username");
                        username = username !=null ? username : displayName;
                    } else {
                        logger.debug("Query Users filter display_name represents tenant_name");
                    }
                }

                PageOfUsers pageOfUsers;

                if (userId != null || cdUserId != null) {
                    OsisUser osisUser = getUser(tenantId,
                            (userId != null) ? userId : cdUserId);
                    pageOfUsers = ScalityModelConverter.toPageOfUsers(osisUser, offset, limit);
                } else {
                    Credentials tempCredentials = getCredentials(tenantId);
                    final AmazonIdentityManagement iam = vaultAdmin.getIAMClient(tempCredentials,
                            appEnv.getRegionInfo().get(0));

                    ListUsersRequest listUsersRequest = ScalityModelConverter.toIAMListUsersRequest(offset, limit);

                    // Add path prefix with osis username to the listusers request if username exists
                    if (username != null && !username.isEmpty()) {
                        listUsersRequest.setPathPrefix("/" + username + "/");
                    }

                    logger.debug("[Vault] List Users Request:{}", new Gson().toJson(listUsersRequest));

                    ListUsersResult listUsersResult = iam.listUsers(listUsersRequest);

                    logger.debug("[Vault] List Users response:{}", new Gson().toJson(listUsersResult));

                    pageOfUsers = ScalityModelConverter.toPageOfUsers(listUsersResult, offset, limit, tenantId);
                    logger.info("Query Users response:{}", new Gson().toJson(pageOfUsers));
                }
                return pageOfUsers;

            } catch (Exception e) {

                if (isAdminPolicyError(e) && !StringUtils.isNullOrEmpty(tenantId)) {
                    try {
                        generateAdminPolicy(tenantId);
                        return queryUsers(offset, limit, filter);
                    } catch (Exception ex) {
                        e = ex;
                    }
                }

                logger.error("Query Users error. Return empty list. Error details: ", e);
                // For errors, Query users should return empty PageOfUsers
                PageInfo pageInfo = new PageInfo(limit, offset);

                PageOfUsers pageOfUsers = new PageOfUsers();
                pageOfUsers.setItems(new ArrayList<>());
                pageOfUsers.setPageInfo(pageInfo);
                return pageOfUsers;
            }
        } else {
            logger.error("QueryUsers requested with invalid filter. Returns empty set of users");
            // For errors, Query Users should return empty PageOfUsers
            PageInfo pageInfo = new PageInfo(limit, offset);

            PageOfUsers pageOfUsers = new PageOfUsers();
            pageOfUsers.setItems(new ArrayList<>());
            pageOfUsers.setPageInfo(pageInfo);
            return pageOfUsers;
        }
    }

    @Override
    public OsisS3Credential createS3Credential(String tenantId, String userId) {
        try {
            logger.info("Create S3 Credential request received:: tenant ID:{}, user ID:{}",
                    tenantId, userId);

            OsisTenant tenant = ScalityModelConverter
                    .toOsisTenant(vaultAdmin.getAccount(ScalityModelConverter.toGetAccountRequestWithID(tenantId)));
            Credentials tempCredentials = getCredentials(tenantId);
            final AmazonIdentityManagement iamClient = vaultAdmin.getIAMClient(tempCredentials,
                    appEnv.getRegionInfo().get(0));

            OsisS3Credential credential = createOsisCredential(tenantId, userId, null, null, iamClient);

            credential.setCdTenantId(tenant.getCdTenantIds().get(0));

            logger.info("Create S3 Credential response:{}, ",
                    ScalityModelConverter.maskSecretKey(new Gson().toJson(credential)));

            return credential;
        } catch (Exception e) {
            if (isAdminPolicyError(e)) {
                try {
                    generateAdminPolicy(tenantId);
                    return createS3Credential(tenantId, userId);
                } catch (Exception ex) {
                    e = ex;
                }
            }

            // Create S3 Credential supports only 400:BAD_REQUEST error
            logger.error("Create S3 Credential error. Error details: ", e);
            throw new VaultServiceException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @Override
    public PageOfS3Credentials queryS3Credentials(long offset, long limit, String filter) {
        logger.info("Query S3 Credentials request received:: filter:{}, offset:{}, limit:{}", filter, offset, limit);
        if (filter.contains(TENANT_ID_PREFIX) && filter.contains(USER_ID_PREFIX)) {
            Map<String, String> kvMap = ScalityUtils.parseFilter(filter);
            String tenantId = kvMap.get(OSIS_TENANT_ID);
            String userId = kvMap.get(OSIS_USER_ID);
            String accessKey = kvMap.get(OSIS_ACCESS_KEY);
            String cdTenantId = kvMap.get(CD_TENANT_ID);

            if (StringUtils.isNullOrEmpty(accessKey)) {
                return listS3Credentials(tenantId, userId, offset, limit);
            } else {

                try {
                    PageOfS3Credentials pageOfS3Credentials = ScalityModelConverter.toPageOfS3Credentials(
                            getS3Credential(tenantId, userId, accessKey, limit),
                            cdTenantId,
                            offset,
                            limit);
                    logger.info("Query S3 Credentials response:{}",
                            ScalityModelConverter.maskSecretKey(new Gson().toJson(pageOfS3Credentials)));
                    return pageOfS3Credentials;

                } catch (Exception e) {
                    logger.error(
                            "Query S3 credential :: The S3 Credential doesn't exist for the given access key. Error details:",
                            e);
                    // For errors, Query Credentials should return empty PageOfS3Credentials
                    return ScalityModelConverter.getEmptyPageOfS3Credentials(offset, limit);
                }
            }
        } else {
            logger.error("QueryS3Credentials requested with invalid filter. Returns empty set of credentials");
            // For errors, Query Credentials should return empty PageOfS3Credentials
            return ScalityModelConverter.getEmptyPageOfS3Credentials(offset, limit);
        }
    }

    @Override
    public String getProviderConsoleUrl() {
        return appEnv.getConsoleEndpoint();
    }

    @Override
    public String getTenantConsoleUrl(String tenantId) {
        return appEnv.getConsoleEndpoint();
        // TODO This has to be changed with S3C console URL in [S3C-3546]
    }

    @Override
    public OsisS3Capabilities getS3Capabilities() {
        logger.info("S3 capabilities request received");
        OsisS3Capabilities osisS3Capabilities = new OsisS3Capabilities();
        String s3CapabilitiesFilePath = appEnv.getS3CapabilitiesFilePath();
        try {
            osisS3Capabilities = new ObjectMapper()
                    .readValue(new ClassPathResource(s3CapabilitiesFilePath).getInputStream(),
                            OsisS3Capabilities.class);
            logger.info("S3 capabilities response:{}", new Gson().toJson(osisS3Capabilities));
        } catch (IOException e) {
            logger.info("Fail to load S3 capabilities from configuration file {}.", s3CapabilitiesFilePath);
        }
        return osisS3Capabilities;
    }

    @Override
    public void deleteS3Credential(String tenantId, String userId, String accessKey) {
        try {
            logger.info("Delete S3 Credential request received:: tenant ID:{}, user ID:{}, accessKey:{}",
                    tenantId, userId, accessKey);
            if (accessKey == null || accessKey.isEmpty()) {
                throw new Exception("accessKey can't be empty for deleteS3Credential.");
            }

            if (tenantId == null || tenantId.isEmpty() || userId == null || userId.isEmpty()) {
                logger.info("Delete S3 Credential: Missing tenantId/userId in request, call Vault superAdmin API GetUserByAccessKey");
                Map<String, String> result = getTenantIdAndUserIdByAccessKeyFromVault(accessKey);
                tenantId = result.get("tenantId");
                userId = result.get("userId");
                logger.info("Delete S3 Credential: GetUserByAccessKey response received, tenant id: {}, user id: {}", tenantId, userId);
            }

            Credentials tempCredentials = getCredentials(tenantId);
            final AmazonIdentityManagement iam = vaultAdmin.getIAMClient(tempCredentials,
                    appEnv.getRegionInfo().get(0));

            DeleteAccessKeyRequest deleteAccessKeyRequest = ScalityModelConverter
                    .toDeleteAccessKeyRequest(accessKey, userId);

            logger.debug("[Vault] Delete Access Key Request:{}", new Gson().toJson(deleteAccessKeyRequest));

            DeleteAccessKeyResult deleteAccessKeyResult = iam.deleteAccessKey(deleteAccessKeyRequest);

            logger.debug("[Vault] Delete Access Key response:{}", new Gson().toJson(deleteAccessKeyResult));

            deleteSecretKey(ScalityModelConverter.toRepoKeyForCredentials(userId, accessKey));

            logger.info("Delete S3 credential successful:: tenant ID:{}, user ID:{}, accessKey:{}",
                    tenantId, userId, accessKey);

        } catch (Exception e) {
            if (isAdminPolicyError(e)) {
                try {
                    generateAdminPolicy(tenantId);
                    deleteS3Credential(tenantId, userId, accessKey);
                } catch (Exception ex) {
                    e = ex;
                }
            }

            logger.error("Delete S3 credential failed. Error details:", e);
        }
    }

    @Override
    public void deleteTenant(String tenantId, Boolean purgeData) {
        throw new NotImplementedException();
    }

    @Override
    public OsisTenant updateTenant(String tenantId, OsisTenant osisTenant) {
        try {
            logger.info("Update Tenant request received, tenantId:{}, osisTenant:{}",
                tenantId, new Gson().toJson(osisTenant));

            // check tenantID and OSIS tenant Consistency
            // special check for ensuring consistency between tenant name and ID
            // the check ensures name and ID in the request belong to the one storage account and its not mis-match
            // this bug was observed as a part of testing OSE v2.2.0.1
            OsisTenant osisTenantFromStoragePlatform = getTenant(tenantId);
            if (!Objects.equals(osisTenant.getName(), osisTenantFromStoragePlatform.getName()) ||
                    !Objects.equals(osisTenant.getTenantId(), osisTenantFromStoragePlatform.getTenantId())) {
                logger.error("Update Tenant failed. Tenant name and tenant ID doesn't match in the request and storage platform");
                throw new VaultServiceException(
                    HttpStatus.BAD_REQUEST,
                    "E_BAD_REQUEST", "Tenant name and tenant ID doesn't match in the request and storage platform"
                );
            }

            UpdateAccountAttributesRequestDTO updateAccountAttributesRequest = ScalityModelConverter
                    .toUpdateAccountAttributesRequestDTO(osisTenant);

            logger.debug("[Vault]Update Account Attributes request:{}",
                    new Gson().toJson(updateAccountAttributesRequest));

            CreateAccountResponseDTO accountResponse = vaultAdmin
                    .updateAccountAttributes(updateAccountAttributesRequest);

            logger.debug("[Vault]Update Account Attributes response:{}", new Gson().toJson(accountResponse));

            OsisTenant resOsisTenant = ScalityModelConverter.toOsisTenant(accountResponse);

            logger.info("Update Tenant response:{}", new Gson().toJson(resOsisTenant));

            return resOsisTenant;
        } catch (VaultServiceException e) {
            // Update Tenant supports only 400:BAD_REQUEST error, change status code in the
            // VaultServiceException
            logger.error("Update Tenant error. Error details: ", e);
            throw new VaultServiceException(HttpStatus.BAD_REQUEST, e.getErrorCode(), e.getReason());
        }
    }

    @Override
    public void deleteUser(String tenantId, String userId, Boolean purgeData) {
        try {
            logger.info("Delete User request received:: tenant ID:{}, userID:{}", tenantId, userId);

            Credentials tempCredentials = getCredentials(tenantId);
            final AmazonIdentityManagement iamClient = vaultAdmin.getIAMClient(tempCredentials,
                    appEnv.getRegionInfo().get(0));

            /** Get userpolicy@<Account_id> **/
            Policy userPolicy = getUserPolicy(iamClient, tenantId);

            if (userPolicy != null) {
                /** Detach user policy from the user **/
                DetachUserPolicyRequest detachUserPolicyRequest = ScalityModelConverter
                        .toDetachUserPolicyRequest(userPolicy.getArn(), userId);
                logger.debug("[Vault] Detach User Policy Request:{}", new Gson().toJson(detachUserPolicyRequest));

                DetachUserPolicyResult detachUserPolicyResult = iamClient.detachUserPolicy(detachUserPolicyRequest);
                logger.debug("[Vault] Detach User Policy response:{}", new Gson().toJson(detachUserPolicyResult));
            }

            DeleteUserRequest deleteUserRequest = ScalityModelConverter.toIAMDeleteUserRequest(userId);

            logger.debug("[Vault] Delete User Request:{}", new Gson().toJson(deleteUserRequest));

            DeleteUserResult deleteUserResult = iamClient.deleteUser(deleteUserRequest);

            logger.debug("[Vault] Delete User response:{}", new Gson().toJson(deleteUserResult));

            logger.info("Delete User successful:: tenant ID:{}, userID:{}", tenantId, userId);
            return;
        } catch (Exception e) {

            if (isAdminPolicyError(e)) {
                try {
                    generateAdminPolicy(tenantId);
                    deleteUser(tenantId, userId, purgeData);
                    return;
                } catch (Exception ex) {
                    e = ex;
                }
            }

            // If delete user fails just return no error response
            logger.error("deleteUser error. User not found. Error details: ", e);
        }

    }

    @Override
    public OsisS3Credential getS3Credential(String accessKey) {
        try {
            logger.info("Get s3 credential request received:: accessKey:{}", accessKey);
            if (accessKey == null || accessKey.isEmpty()) {
                throw new Exception("accessKey can't be empty for getS3Credential.");
            }
            logger.info("Get S3 Credential: call Vault superAdmin API GetUserByAccessKey");
            Map<String, String> result = getTenantIdAndUserIdByAccessKeyFromVault(accessKey);
            final String tenantId = result.get("tenantId");
            final String userId = result.get("userId");
            logger.info("Get S3 Credential: GetUserByAccessKey response received, tenant id: {}, user id: {}", tenantId, userId);

            return getS3Credential(tenantId, userId, accessKey);

        } catch (Exception e) {
            logger.error(
                    "Get S3 credential :: The S3 Credential doesn't exist for the given access key. Error details:",
                    e);
            throw new VaultServiceException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @Override
    public OsisS3Credential getS3Credential(String tenantId, String userId, String accessKey) {
        return getS3Credential(tenantId, userId, accessKey, DEFAULT_MAX_LIMIT);
    }

    private OsisS3Credential getS3Credential(String tenantId, String userId, String accessKey, long limit) {
        try {
            logger.info("Get s3 credential request received:: tenant ID:{}, user ID:{}, accessKey:{}",
                    tenantId, userId, accessKey);
            if (accessKey == null || accessKey.isEmpty()) {
                throw new Exception("accessKey can't be empty for getS3Credential.");
            }
            if (tenantId == null || tenantId.isEmpty() || userId == null || userId.isEmpty()) {
                logger.info("Get S3 Credential: Missing tenantId/userId in request, call Vault superAdmin API GetUserByAccessKey");
                Map<String, String> result = getTenantIdAndUserIdByAccessKeyFromVault(accessKey);
                tenantId = result.get("tenantId");
                userId = result.get("userId");
                logger.info("Get S3 Credential: GetUserByAccessKey response received, tenant id: {}, user id: {}", tenantId, userId);
            }

            Credentials tempCredentials = getCredentials(tenantId);
            final AmazonIdentityManagement iam = vaultAdmin.getIAMClient(tempCredentials,
                    appEnv.getRegionInfo().get(0));

            ListAccessKeysRequest listAccessKeysRequest = ScalityModelConverter.toIAMListAccessKeysRequest(userId,
                    limit);

            logger.debug("[Vault] List Access Keys Request:{}", new Gson().toJson(listAccessKeysRequest));

            ListAccessKeysResult listAccessKeysResult = iam.listAccessKeys(listAccessKeysRequest);

            logger.debug("[Vault] List Access Keys response:{}", new Gson().toJson(listAccessKeysResult));

            Optional<AccessKeyMetadata> accessKeyResult = listAccessKeysResult.getAccessKeyMetadata()
                    .stream()
                    .filter(accessKeyMetadata -> accessKeyMetadata.getAccessKeyId().equals(accessKey))
                    .findAny();

            if (accessKeyResult.isPresent()) {
                AccessKeyMetadata accessKeyMetadata = accessKeyResult.get();

                String secretKey = retrieveSecretKey(
                        ScalityModelConverter.toRepoKeyForCredentials(userId, accessKeyMetadata.getAccessKeyId()));

                // get Osis User by userId
                GetUserRequest getUserRequest = ScalityModelConverter.toIAMGetUserRequest(userId);
                logger.debug("[Vault] Get User Request:{}", new Gson().toJson(getUserRequest));
                GetUserResult getUserResult = iam.getUser(getUserRequest);
                logger.debug("[Vault] Get User response:{}", new Gson().toJson(getUserResult));
                OsisUser osisUser = ScalityModelConverter.toOsisUser(getUserResult.getUser(), tenantId);

                OsisS3Credential osisCredential = ScalityModelConverter.toOsisS3Credentials(tenantId,
                        osisUser.getCdTenantId(),
                        accessKeyMetadata,
                        secretKey);
                logger.info("Get S3 credential  response:{}",
                        ScalityModelConverter.maskSecretKey(new Gson().toJson(osisCredential)));

                return osisCredential;
            } else {
                throw new NotFoundException("The S3 Credential doesn't exist for the given access key");
            }

        } catch (Exception e) {

            if (isAdminPolicyError(e)) {
                try {
                    generateAdminPolicy(tenantId);
                    return getS3Credential(tenantId, userId, accessKey);
                } catch (Exception ex) {
                    e = ex;
                }
            }
            logger.error(
                    "Get S3 credential :: The S3 Credential doesn't exist for the given access key. Error details:",
                    e);
            throw new VaultServiceException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @Override
    public OsisTenant getTenant(String tenantId) {
        try {
            logger.info("Get Tenant request received, tenantId:{}", tenantId);
            GetAccountRequestDTO getAccountRequest = ScalityModelConverter.toGetAccountRequestWithID(tenantId);

            logger.debug("[Vault]GetAccount request:{}", new Gson().toJson(getAccountRequest));

            AccountData accountData = vaultAdmin.getAccount(getAccountRequest);

            logger.debug("[Vault]GetAccount response:{}", new Gson().toJson(accountData));

            OsisTenant resOsisTenant;
            resOsisTenant = ScalityModelConverter.toOsisTenant(accountData);
            logger.info("Get Tenant response:{}", new Gson().toJson(resOsisTenant));

            return resOsisTenant;
        } catch (Exception e) {
            logger.error("The tenant doesn't exist. Error details: ", e);
            throw new VaultServiceException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @Override
    public OsisUser getUser(String canonicalUserId) {
        try {
            logger.info("Get User w/ Canonical ID request received:: canonicalUserId ID:{}", canonicalUserId);

            GetAccountRequestDTO getAccountRequest = ScalityModelConverter
                    .toGetAccountRequestWithCanonicalID(canonicalUserId);

            logger.debug("[Vault] Get Account Request:{}", new Gson().toJson(getAccountRequest));

            AccountData account = vaultAdmin.getAccount(getAccountRequest);

            logger.debug("[Vault] Get Account response:{}", new Gson().toJson(account));

            List<OsisUser> users = listUsers(account.getId(), DEFAULT_MIN_OFFSET, DEFAULT_MAX_LIMIT).getItems();

            final OsisUser osisUser = ScalityModelConverter.toCanonicalOsisUser(account, users);

            // List all user access keys and if all are inactive, mark user as inactive
            Credentials tempCredentials = getCredentials(account.getId());
            final AmazonIdentityManagement iamClient = vaultAdmin.getIAMClient(tempCredentials,
                    appEnv.getRegionInfo().get(0));

            ListAccessKeysRequest listAccessKeysRequest = ScalityModelConverter
                    .toIAMListAccessKeysRequest(osisUser.getUserId(), DEFAULT_MAX_LIMIT);

            logger.debug("[Vault] List Access Keys Request:{}", new Gson().toJson(listAccessKeysRequest));

            ListAccessKeysResult listAccessKeysResult = iamClient.listAccessKeys(listAccessKeysRequest);

            logger.debug("[Vault] List Access Keys response:{}", new Gson().toJson(listAccessKeysResult));

            boolean isActive = false;
            for (AccessKeyMetadata accessKey : listAccessKeysResult.getAccessKeyMetadata()) {
                if (accessKey.getStatus().equals(StatusType.Active.toString())) {
                    isActive = true;
                    break;
                }
            }

            osisUser.setActive(isActive);

            logger.info("Get User w/ Canonical ID response:{}", new Gson().toJson(osisUser));

            return osisUser;
        } catch (Exception e) {
            logger.error("The tenant doesn't exist. Error details: ", e);
            throw new VaultServiceException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @Override
    public OsisUser getUser(String tenantId, String userId) {
        try {
            logger.info("Get User request received:: tenant ID:{}, userID:{}", tenantId, userId);

            Credentials tempCredentials = getCredentials(tenantId);
            final AmazonIdentityManagement iamClient = vaultAdmin.getIAMClient(tempCredentials,
                    appEnv.getRegionInfo().get(0));

            GetUserRequest getUserRequest = ScalityModelConverter.toIAMGetUserRequest(userId);

            logger.debug("[Vault] Get User Request:{}", new Gson().toJson(getUserRequest));

            GetUserResult getUserResult = iamClient.getUser(getUserRequest);

            logger.debug("[Vault] Get User response:{}", new Gson().toJson(getUserResult));

            OsisUser osisUser = ScalityModelConverter.toOsisUser(getUserResult.getUser(), tenantId);

            // List all user access keys and if all are inactive, mark user as inactive
            ListAccessKeysRequest listAccessKeysRequest = ScalityModelConverter.toIAMListAccessKeysRequest(userId,
                    DEFAULT_MAX_LIMIT);

            logger.debug("[Vault] List Access Keys Request:{}", new Gson().toJson(listAccessKeysRequest));

            ListAccessKeysResult listAccessKeysResult = iamClient.listAccessKeys(listAccessKeysRequest);

            logger.debug("[Vault] List Access Keys response:{}", new Gson().toJson(listAccessKeysResult));

            boolean isActive = false;
            for (AccessKeyMetadata accessKey : listAccessKeysResult.getAccessKeyMetadata()) {
                if (accessKey.getStatus().equals(StatusType.Active.toString())) {
                    isActive = true;
                    break;
                }
            }

            osisUser.setActive(isActive);

            logger.info("Get User response:{}", new Gson().toJson(osisUser));

            return osisUser;
        } catch (Exception e) {

            if (isAdminPolicyError(e)) {
                try {
                    generateAdminPolicy(tenantId);
                    return getUser(tenantId, userId);
                } catch (Exception ex) {
                    e = ex;
                }
            }

            logger.error("GetUser error. User not found. Error details: ", e);
            throw new VaultServiceException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }

    }

    @Override
    public boolean headTenant(String tenantId) {
        try {
            logger.info("Head Tenant request received:: tenant ID:{}", tenantId);
            AccountData accountData = vaultAdmin.getAccount(ScalityModelConverter.toGetAccountRequestWithID(tenantId));
            logger.info("Head Tenant response:: {}", accountData);
            return accountData != null && accountData.getId().equals(tenantId);
        } catch (Exception e) {
            logger.error("Head Tenant error. Error details: ", e);
            return false;
        }
    }

    @Override
    public boolean headUser(String tenantId, String userId) {
        try {
            logger.info("Head User request received:: tenant ID:{} user ID:{}", tenantId, userId);
            Credentials tempCredentials = getCredentials(tenantId);

            final AmazonIdentityManagement iamClient = vaultAdmin.getIAMClient(tempCredentials, appEnv.getRegionInfo().get(0));

            GetUserRequest getUserRequest = ScalityModelConverter.toIAMGetUserRequest(userId);

            logger.debug("[Vault] Get User Request:{}", new Gson().toJson(getUserRequest));

            GetUserResult getUserResult = iamClient.getUser(getUserRequest);
            logger.info("Head User response:: {}", getUserResult.getUser());
            return getUserResult.getUser() != null && getUserResult.getUser().getUserName().equals(userId);
        } catch (Exception e) {
            logger.error("Head User error. Error details: ", e);
            throw new VaultServiceException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @Override
    public PageOfS3Credentials listS3Credentials(String tenantId, String userId, Long offset, Long limit) {
        try {
            OsisTenant tenant = ScalityModelConverter
                    .toOsisTenant(vaultAdmin.getAccount(ScalityModelConverter.toGetAccountRequestWithID(tenantId)));
            logger.info("List s3 credentials request received:: tenant ID:{}, user ID:{}, offset:{}, limit:{}",
                    tenantId, userId, offset, limit);

            Credentials tempCredentials = getCredentials(tenantId);
            final AmazonIdentityManagement iam = vaultAdmin.getIAMClient(tempCredentials,
                    appEnv.getRegionInfo().get(0));

            ListAccessKeysRequest listAccessKeysRequest = ScalityModelConverter.toIAMListAccessKeysRequest(userId,
                    limit);

            logger.debug("[Vault] List Access Keys Request:{}", new Gson().toJson(listAccessKeysRequest));

            ListAccessKeysResult listAccessKeysResult = iam.listAccessKeys(listAccessKeysRequest);

            logger.debug("[Vault] List Access Keys response:{}", new Gson().toJson(listAccessKeysResult));

            Map<String, String> secretKeyMap = new HashMap<>();
            for (AccessKeyMetadata accessKey : listAccessKeysResult.getAccessKeyMetadata()) {
                String secretKey = retrieveSecretKey(
                        ScalityModelConverter.toRepoKeyForCredentials(userId, accessKey.getAccessKeyId()));
                if (!StringUtils.isNullOrEmpty(secretKey)) {
                    secretKeyMap.put(accessKey.getAccessKeyId(), secretKey);
                }
            }

            // If no secret keys are present in Redis, create a new key and add it to
            // secretKeyMap
            if (secretKeyMap.isEmpty()) {
                CreateAccessKeyResult createAccessKeyResult = createAccessKey(userId, iam);

                AccessKeyMetadata newAccessKeyMetadata = ScalityModelConverter
                        .toAccessKeyMetadata(createAccessKeyResult.getAccessKey());
                listAccessKeysResult.getAccessKeyMetadata().add(newAccessKeyMetadata);

                secretKeyMap.put(createAccessKeyResult.getAccessKey().getAccessKeyId(),
                        createAccessKeyResult.getAccessKey().getSecretAccessKey());
            }

            PageOfS3Credentials pageOfS3Credentials = ScalityModelConverter
                    .toPageOfS3Credentials(listAccessKeysResult, offset, limit, tenant, secretKeyMap);
            logger.info("List S3 credentials  response:{}",
                    ScalityModelConverter.maskSecretKey(new Gson().toJson(pageOfS3Credentials)));

            pageOfS3Credentials.getItems()
                    .forEach(s3Credential -> s3Credential.setCdTenantId(tenant.getCdTenantIds().get(0)));
            return pageOfS3Credentials;
        } catch (Exception e) {

            if (isAdminPolicyError(e)) {
                try {
                    generateAdminPolicy(tenantId);
                    return listS3Credentials(tenantId, userId, offset, limit);
                } catch (Exception ex) {
                    e = ex;
                }
            }

            logger.error("ListS3Credentials error. Returning empty list. Error details: ", e);
            // For errors, ListS3Credentials should return empty PageOfS3Credentials

            return ScalityModelConverter.getEmptyPageOfS3Credentials(offset, limit);
        }
    }

    @Override
    public OsisS3Credential updateCredentialStatus(String tenantId, String userId, String accessKey, OsisS3Credential osisS3Credential) {
        String tenantIdOfCurrentUser = tenantId != null && !tenantId.isEmpty() ? tenantId : osisS3Credential.getTenantId();
        String userIdOfCurrentUser = userId != null && !userId.isEmpty() ? userId : osisS3Credential.getUserId();
        logger.info("UpdateCredentialStatus request received:: tenant ID:{}, user ID:{}, accessKey:{}, isActive: {}",
                tenantIdOfCurrentUser, userIdOfCurrentUser, accessKey, osisS3Credential.getActive());

        try {
            if (tenantIdOfCurrentUser == null || tenantIdOfCurrentUser.isEmpty()
                    || userIdOfCurrentUser == null || userIdOfCurrentUser.isEmpty()) {
                logger.info("UpdatedCredentialStatus: Missing tenantId/userId in both parameter and request body, call Vault superAdmin API GetUserByAccessKey");
                Map<String, String> result = getTenantIdAndUserIdByAccessKeyFromVault(accessKey);
                tenantIdOfCurrentUser = result.get("tenantId");
                userIdOfCurrentUser = result.get("userId");
                logger.info("UpdatedCredentialStatus: GetUserByAccessKey response received, tenant id: {}, user id: {}", tenantIdOfCurrentUser, userIdOfCurrentUser);
            }

            Credentials tempCredentials = getCredentials(tenantIdOfCurrentUser);
            final AmazonIdentityManagement iam = vaultAdmin.getIAMClient(tempCredentials,
                    appEnv.getRegionInfo().get(0));
            UpdateAccessKeyRequest updateAccessKeyRequest = ScalityModelConverter.toIAMUpdateAccessKeyRequest(
                    userIdOfCurrentUser,
                    accessKey,
                    osisS3Credential.getActive());
            iam.updateAccessKey(updateAccessKeyRequest);
            OsisS3Credential newOsisS3Credential = this.getS3Credential(tenantIdOfCurrentUser, userIdOfCurrentUser, accessKey);
            logger.info("UpdatedCredentialStatus response:{}", ScalityModelConverter.maskSecretKey(new Gson().toJson(newOsisS3Credential)));
            return newOsisS3Credential;
        } catch (Exception e) {
            if (isAdminPolicyError(e)) {
                try {
                    generateAdminPolicy(tenantIdOfCurrentUser);
                    return updateCredentialStatus(tenantIdOfCurrentUser, userIdOfCurrentUser, accessKey, osisS3Credential);
                } catch (Exception ex) {
                    e = ex;
                }
            }

            logger.error("UpdateCredentialStatus error. Error details: ", e);
            throw new VaultServiceException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @Override
    public PageOfUsers listUsers(String tenantId, long offset, long limit) {
        try {
            logger.info("List Users request received:: tenant ID:{}, offset:{}, limit:{}", tenantId, offset, limit);

            Credentials tempCredentials = getCredentials(tenantId);
            final AmazonIdentityManagement iam = vaultAdmin.getIAMClient(tempCredentials,
                    appEnv.getRegionInfo().get(0));

            ListUsersRequest listUsersRequest = ScalityModelConverter.toIAMListUsersRequest(offset, limit);

            logger.debug("[Vault] List Users Request:{}", new Gson().toJson(listUsersRequest));

            ListUsersResult listUsersResult = iam.listUsers(listUsersRequest);

            logger.debug("[Vault] List Users response:{}", new Gson().toJson(listUsersResult));

            PageOfUsers pageOfUsers = ScalityModelConverter.toPageOfUsers(listUsersResult, offset, limit, tenantId);

            for (OsisUser osisUser : pageOfUsers.getItems()) {
                // List all user access keys and if all are inactive, mark user as inactive
                ListAccessKeysRequest listAccessKeysRequest = ScalityModelConverter
                        .toIAMListAccessKeysRequest(osisUser.getUserId(), DEFAULT_MAX_LIMIT);

                logger.debug("[Vault] List Access Keys Request:{}", new Gson().toJson(listAccessKeysRequest));

                ListAccessKeysResult listAccessKeysResult = iam.listAccessKeys(listAccessKeysRequest);

                logger.debug("[Vault] List Access Keys response:{}", new Gson().toJson(listAccessKeysResult));

                boolean isActive = false;
                for (AccessKeyMetadata accessKey : listAccessKeysResult.getAccessKeyMetadata()) {
                    if (accessKey.getStatus().equals(StatusType.Active.toString())) {
                        isActive = true;
                        break;
                    }
                }
                osisUser.setActive(isActive);
            }
            logger.info("List Users response:{}", new Gson().toJson(pageOfUsers));

            return pageOfUsers;
        } catch (Exception e) {

            if (isAdminPolicyError(e)) {
                try {
                    generateAdminPolicy(tenantId);
                    return listUsers(tenantId, offset, limit);
                } catch (Exception ex) {
                    e = ex;
                }
            }

            logger.error("ListUsers error. Returning empty list. Error details: ", e);
            // For errors, List Users should return empty PageOfUsers
            PageInfo pageInfo = new PageInfo(limit, offset);

            PageOfUsers pageOfUsers = new PageOfUsers();
            pageOfUsers.setItems(new ArrayList<>());
            pageOfUsers.setPageInfo(pageInfo);
            return pageOfUsers;
        }

    }

    @Override
    public OsisUser updateUser(String tenantId, String userId, OsisUser osisUser) {
        try {
            OsisTenant tenant = ScalityModelConverter
                    .toOsisTenant(vaultAdmin.getAccount(ScalityModelConverter.toGetAccountRequestWithID(tenantId)));
            logger.info("Update User request received:: tenant ID:{}, user ID:{}", tenantId, userId);

            Credentials tempCredentials = getCredentials(tenantId);
            final AmazonIdentityManagement iam = vaultAdmin.getIAMClient(tempCredentials,
                    appEnv.getRegionInfo().get(0));

            // List all access keys for the user
            ListAccessKeysRequest listAccessKeysRequest = ScalityModelConverter.toIAMListAccessKeysRequest(userId,
                    DEFAULT_MAX_LIMIT);

            logger.debug("[Vault] List Access Keys Request:{}", new Gson().toJson(listAccessKeysRequest));

            ListAccessKeysResult listAccessKeysResult = iam.listAccessKeys(listAccessKeysRequest);

            logger.debug("[Vault] List Access Keys response:{}", new Gson().toJson(listAccessKeysResult));

            for (AccessKeyMetadata accessKey : listAccessKeysResult.getAccessKeyMetadata()) {
                // Update each access key of the user to active/inactive
                UpdateAccessKeyRequest updateAccessKeyRequest = ScalityModelConverter.toIAMUpdateAccessKeyRequest(
                        userId,
                        accessKey.getAccessKeyId(), osisUser.getActive());
                iam.updateAccessKey(updateAccessKeyRequest);
            }

            logger.info("Updated user response:{}", ScalityModelConverter.maskSecretKey(new Gson().toJson(osisUser)));
            return osisUser;
        } catch (Exception e) {

            if (isAdminPolicyError(e)) {
                try {
                    generateAdminPolicy(tenantId);
                    return updateUser(tenantId, userId, osisUser);
                } catch (Exception ex) {
                    e = ex;
                }
            }

            logger.error("Update User error. Error details: ", e);
            throw new VaultServiceException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @Override
    public Information getInformation(String domain) {
        logger.info("Get Information request received:: domain:{}", domain);
        Information information = new Information()
                .addAuthModesItem(
                        appEnv.isApiTokenEnabled() ? Information.AuthModesEnum.BEARER : Information.AuthModesEnum.BASIC)
                .storageClasses(appEnv.getStorageInfo())
                .regions(appEnv.getRegionInfo())
                .platformName(appEnv.getPlatformName())
                .platformVersion(appEnv.getPlatformVersion())
                .apiVersion(appEnv.getApiVersion())
                .notImplemented(scalityOsisCapsManager.getNotImplements())
                .logoUri(ScalityUtils.getLogoUri(domain))
                .services(new InformationServices().iam(domain + IAM_PREFIX).s3(appEnv.getS3Endpoint()))
                .status(Information.StatusEnum.NORMAL);
        logger.info("Get Information response: {}", new Gson().toJson(information));
        return information;
    }

    @Override
    public ScalityOsisCaps updateOsisCaps(ScalityOsisCaps osisCaps) {
        throw new NotImplementedException();
    }

    @Override
    public PageOfOsisBucketMeta getBucketList(String tenantId, long offset, long limit) {
        try {
            logger.info("Get bucket list request received:: tenant ID:{}, offset:{}, limit:{}", tenantId, offset, limit);
            Credentials tempCredentials = getCredentials(tenantId);
            final AmazonS3 s3Client = this.s3.getS3Client(tempCredentials,
                    appEnv.getRegionInfo().get(0));

            //s3 listBucket has no pagination, so list all
            List<Bucket> buckets = s3Client.listBuckets();

            logger.debug("[S3] List all Buckets size:{}", buckets.size());

            final PageOfUsers pageOfUsers = listUsers(tenantId, 0, 1);
            final String userId = pageOfUsers.getItems().get(0).getUserId();

            PageOfOsisBucketMeta pageOfOsisBucketMeta = ScalityModelConverter.toPageOfOsisBucketMeta(buckets, userId, offset, limit);

            logger.info("List Buckets response:{}", new Gson().toJson(pageOfOsisBucketMeta));

            return pageOfOsisBucketMeta;
        } catch (Exception e) {

            if (isAdminPolicyError(e)) {
                try {
                    generateAdminPolicy(tenantId);
                    return getBucketList(tenantId, offset, limit);
                } catch (Exception ex) {
                    e = ex;
                }
            }

            logger.error("GetBucketList error. Returning empty list. Error details: ", e);
            // For errors, GetBucketList should return empty PageOfOsisBucketMeta
            PageInfo pageInfo = new PageInfo(limit, offset);

            PageOfOsisBucketMeta pageOfOsisBucketMeta = new PageOfOsisBucketMeta();
            pageOfOsisBucketMeta.setItems(new ArrayList<>());
            pageOfOsisBucketMeta.setPageInfo(pageInfo);
            return pageOfOsisBucketMeta;
        }

    }

    @Override
    public OsisUsage getOsisUsage(Optional<String> tenantId, Optional<String> userId) {
        return new OsisUsage();
    }

    @Override
    public AnonymousUser getAnonymousUser() {
        logger.info("Get Anonymous User request received");
        AnonymousUser anonymousUser = new AnonymousUser()
                .id(ANONYMOUS_USER_ID)
                .name(ANONYMOUS_USER_NAME);
        logger.trace("Get Anonymous User response, {}", new Gson().toJson(anonymousUser));
        return anonymousUser;
    }

    /**
     * Gets credentials.
     *
     * @param accountID the account id
     * @return the credentials
     */
    public Credentials getCredentials(String accountID) {
        Credentials credentials = null;
        try {
            AssumeRoleRequest assumeRoleRequest = ScalityModelConverter.getAssumeRoleRequestForAccount(accountID,
                    appEnv.getAssumeRoleName());
            logger.debug("[Vault] Assume Role request:{}", assumeRoleRequest);
            credentials = vaultAdmin.getTempAccountCredentials(assumeRoleRequest);
            logger.debug("[Vault] Assume Role response received with access key:{}, expiration:{}",
                    credentials.getAccessKeyId(), credentials.getExpiration());
        } catch (VaultServiceException e) {

            if (!StringUtils.isNullOrEmpty(e.getErrorCode()) &&
                    NO_SUCH_ENTITY_ERR.equals(e.getErrorCode()) &&
                    ROLE_DOES_NOT_EXIST_ERR.equals(e.getReason())) {
                // If role does not exists, invoke setupAssumeRole
                logger.error(ROLE_DOES_NOT_EXIST_ERR + ". Recreating the role");
                // Call get Account with Account ID to retrieve account name
                AccountData account = vaultAdmin.getAccount(ScalityModelConverter.toGetAccountRequestWithID(accountID));
                asyncScalityOsisService.setupAssumeRole(accountID, account.getName());
                return getCredentials(accountID);
            }
            throw e;
        }
        return credentials;
    }

    /**
     * Create osis credential osis s 3 credential.
     *
     * @param tenantId   the tenant id
     * @param userId     the user id
     * @param cdTenantId the cd tenant id
     * @param username   the username
     * @param iam        the iam
     * @return the osis s 3 credential
     */
    public OsisS3Credential createOsisCredential(String tenantId, String userId, String cdTenantId, String username,
            AmazonIdentityManagement iam) throws Exception {

        CreateAccessKeyResult createAccessKeyResult = createAccessKey(userId, iam);

        return ScalityModelConverter.toOsisS3Credentials(cdTenantId,
                tenantId,
                username,
                createAccessKeyResult);
    }

    /**
     * Create access key on iam.
     *
     * @param userId the user id
     * @param iam    the iam
     * @return the iam access key
     */
    private CreateAccessKeyResult createAccessKey(String userId, AmazonIdentityManagement iam) throws Exception {

        CreateAccessKeyRequest createAccessKeyRequest = ScalityModelConverter.toCreateUserAccessKeyRequest(userId);

        logger.debug("[Vault] Create User Access Key Request:{}", new Gson().toJson(createAccessKeyRequest));

        CreateAccessKeyResult createAccessKeyResult = iam.createAccessKey(createAccessKeyRequest);

        logger.debug("[Vault] Create User Access Key Response:{}", createAccessKeyResult);

        storeSecretKey(
                ScalityModelConverter.toRepoKeyForCredentials(userId,
                        createAccessKeyResult.getAccessKey().getAccessKeyId()),
                createAccessKeyResult.getAccessKey().getSecretAccessKey());

        return createAccessKeyResult;
    }

    public Policy getOrCreateUserPolicy(AmazonIdentityManagement iam, String tenantId) {
        Policy userPolicy = getUserPolicy(iam, tenantId);

        if (userPolicy == null || StringUtils.isNullOrEmpty(userPolicy.getArn())) {
            /** Policy does not exists **/
            logger.debug("[Vault] User policy does not exists. A new user policy will be created");

            /** Create a new policy with necessary permissions **/
            CreatePolicyRequest createPolicyRequest = ScalityModelConverter.toCreateUserPolicyRequest(tenantId);
            logger.debug("[Vault] Create Policy Request:{}", new Gson().toJson(createPolicyRequest));

            CreatePolicyResult createPolicyResult = iam.createPolicy(createPolicyRequest);
            logger.debug("[Vault] Create Policy response:{}", new Gson().toJson(createPolicyResult));

            userPolicy = createPolicyResult.getPolicy();

        }
        return userPolicy;
    }

    private Policy getUserPolicy(AmazonIdentityManagement iam, String tenantId) {
        try {
            GetPolicyRequest getPolicyRequest = ScalityModelConverter.toGetPolicyRequest(tenantId);

            logger.debug("[Vault] Get Policy Request:{}", new Gson().toJson(getPolicyRequest));

            GetPolicyResult getPolicyResult = iam.getPolicy(getPolicyRequest);

            logger.debug("[Vault] Get Policy response:{}", new Gson().toJson(getPolicyResult));

            return getPolicyResult.getPolicy();

        } catch (com.amazonaws.services.identitymanagement.model.NoSuchEntityException e) {
            return null;
        }
    }

    private void generateAdminPolicy(String tenantId) throws Exception {
        AccountData account = vaultAdmin.getAccount(ScalityModelConverter.toGetAccountRequestWithID(tenantId));
        asyncScalityOsisService.setupAdminPolicy(tenantId, account.getName(), appEnv.getAssumeRoleName());
    }

    private boolean isAdminPolicyError(Exception e) {
        return (e instanceof AmazonIdentityManagementException &&
                (HttpStatus.FORBIDDEN.value() == ((AmazonIdentityManagementException) e).getStatusCode()))
                ||
                (e instanceof AmazonS3Exception && (HttpStatus.FORBIDDEN.value() == ((AmazonS3Exception) e).getStatusCode()));
    }

    private void storeSecretKey(String repoKey, String secretAccessKey) throws Exception {
        // Using `repoKey` for Associated Data during encryption
        logger.debug("[Cache] Store Secret Key on cache. Key:{}", repoKey);
        SecretKeyRepoData encryptedRepoData = cipherFactory.getCipher().encrypt(secretAccessKey,
                cipherFactory.getLatestSecretCipherKey(),
                repoKey);

        encryptedRepoData.setKeyID(cipherFactory.getLatestCipherID());
        encryptedRepoData.getCipherInfo().setCipherName(cipherFactory.getLatestCipherName());
        // Prefix Cipher ID to the encrypted value

        if (REDIS_SPRING_CACHE_TYPE.equalsIgnoreCase(appEnv.getSpringCacheType())) {
            scalityRedisRepository.save(repoKey, encryptedRepoData);
        } else {
            springLocalCache.put(repoKey, encryptedRepoData);
        }
        logger.debug("[Cache] Store Secret Key successful");
    }

    private String retrieveSecretKey(String repoKey) throws Exception {
        logger.debug("[Cache] Retrieve Secret Key from cache. Key:{}", repoKey);
        SecretKeyRepoData repoVal = null;
        if (REDIS_SPRING_CACHE_TYPE.equalsIgnoreCase(appEnv.getSpringCacheType())) {
            if (scalityRedisRepository.hasKey(repoKey)) {
                repoVal = scalityRedisRepository.get(repoKey);
            }
        } else {
            repoVal = springLocalCache.get(repoKey);
        }

        String secretKey = null;

        if (repoVal != null) {

            // Using `repoKey` for Associated Data during encryption
            secretKey = cipherFactory.getCipherByID(repoVal.getKeyID())
                    .decrypt(repoVal,
                            cipherFactory.getSecretCipherKeyByID(repoVal.getKeyID()),
                            repoKey);

            logger.debug("[Cache] Retrieve Secret Key successful");
        }
        return secretKey;
    }

    private void deleteSecretKey(String repoKey) throws Exception {
        logger.debug("[Cache] Delete Secret Key from cache. Key:{}", repoKey);
        if (REDIS_SPRING_CACHE_TYPE.equalsIgnoreCase(appEnv.getSpringCacheType())) {
            if (scalityRedisRepository.hasKey(repoKey)) {
                scalityRedisRepository.delete(repoKey);
            }
        } else {
            springLocalCache.remove(repoKey);
        }
        logger.debug("[Cache] Delete Secret Key from cache successful");
    }

    private Map<String, String> getTenantIdAndUserIdByAccessKeyFromVault(String accessKey) {

        GetUserByAccessKeyRequestDTO getUserByAccessKeyRequest = ScalityModelConverter.toScalityGetUserByAccessKeyRequest(accessKey);

        GetUserByAccessKeyResponseDTO getUserByAccessKeyResponse = vaultAdmin.getUserByAccessKey(getUserByAccessKeyRequest);

        String tenantId = getUserByAccessKeyResponse.getData().getParentId();
        String userId = getUserByAccessKeyResponse.getData().getName();

        Map<String, String> result = new HashMap<>();
        result.put("tenantId", tenantId);
        result.put("userId", userId);
        return result;
    }
}
