/**
 * Copyright 2020 VMware, Inc.
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.service.impl;

import com.amazonaws.Response;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.scality.osis.ScalityAppEnv;
import com.scality.osis.model.*;
import com.scality.osis.model.exception.NotFoundException;
import com.scality.osis.model.exception.NotImplementedException;
import com.scality.osis.resource.ScalityOsisCapsManager;
import com.scality.osis.service.ScalityOsisService;
import com.scality.osis.service.credentials.SecretKeyStore;
import com.scality.osis.service.tenant.TenantSession;
import com.scality.osis.utapi.Utapi;
import com.scality.osis.utapiclient.dto.ListMetricsRequestDTO;
import com.scality.osis.utapiclient.dto.MetricsData;
import com.scality.osis.utapiclient.services.UtapiServiceClient;
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

import static com.scality.osis.utils.ScalityConstants.*;
import static com.scality.osis.utils.ScalityUtils.getHourTime;

/**
 * The type Scality osis service.
 */
@Service
@Primary
public class ScalityOsisServiceImpl implements ScalityOsisService {
    private static final Logger logger = LoggerFactory.getLogger(ScalityOsisServiceImpl.class);

    private ScalityAppEnv appEnv;
    private VaultAdmin vaultAdmin;
    private Utapi utapi;
    private ScalityOsisCapsManager scalityOsisCapsManager;

    @Autowired
    private AsyncScalityOsisService asyncScalityOsisService;

    @Autowired
    private SecretKeyStore secretKeyStore;

    @Autowired
    private TenantSession tenantSession;

    /**
     * Instantiates a new Scality osis service.
     *
     * @param appEnv                 the app env
     * @param vaultAdmin             the vault admin
     * @param utapi                  the utapi client
     * @param scalityOsisCapsManager the osis caps manager
     */
    public ScalityOsisServiceImpl(ScalityAppEnv appEnv, VaultAdmin vaultAdmin,
                                  Utapi utapi,
                                  ScalityOsisCapsManager scalityOsisCapsManager) {
        this.appEnv = appEnv;
        this.vaultAdmin = vaultAdmin;
        this.utapi = utapi;
        this.scalityOsisCapsManager = scalityOsisCapsManager;
    }

    /**
     * Builds the exception to surface for a failed create/update operation. A genuine Vault server
     * fault (a {@link VaultServiceException} with a 5xx status) is returned unchanged so the error
     * boundary logs it once at ERROR with the stack trace. Every other failure is treated as a
     * client-side rejection and mapped to {@code 400 BAD_REQUEST}, keeping the original as the cause.
     */
    private VaultServiceException toResponseException(Exception e) {
        if (e instanceof VaultServiceException
                && ((VaultServiceException) e).getStatus().is5xxServerError()) {
            return (VaultServiceException) e;
        }
        return new VaultServiceException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
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
            // A client-side Vault rejection maps to 400; a genuine Vault server fault (5xx) is
            // preserved so the boundary logs it once at ERROR with the trace.
            throw toResponseException(e);
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
                logger.warn("Query Tenants failed; returning empty list: {}", e.getMessage());
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
            logger.warn("List Tenants failed; returning empty list: {}", e.getMessage());
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
            return tenantSession.run(osisUser.getTenantId(), () -> {
                logger.info("Create User request received:{}", new Gson().toJson(osisUser));

                AccountData accountData = vaultAdmin
                        .getAccount(ScalityModelConverter.toGetAccountRequestWithID(osisUser.getTenantId()));
                osisUser.setCanonicalUserId(accountData.getCanonicalId());

                final AmazonIdentityManagement iam = tenantSession.getIamClient(osisUser.getTenantId());

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
            });
        } catch (Exception e) {
            // A client-side Vault rejection maps to 400; a genuine Vault server fault (5xx) is
            // preserved so the boundary logs it once at ERROR with the trace.
            throw toResponseException(e);
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
                        username = username != null ? username : displayName;
                    } else {
                        logger.debug("Query Users filter display_name represents tenant_name");
                    }
                }

                final String resolvedTenantId = tenantId;
                final String resolvedUserId = userId;
                final String resolvedCdUserId = cdUserId;
                final String resolvedUsername = username;

                return tenantSession.run(resolvedTenantId, () -> {
                    PageOfUsers pageOfUsers;

                    if (resolvedUserId != null || resolvedCdUserId != null) {
                        OsisUser osisUser = getUser(resolvedTenantId,
                                (resolvedUserId != null) ? resolvedUserId : resolvedCdUserId);
                        pageOfUsers = ScalityModelConverter.toPageOfUsers(osisUser, offset, limit);
                    } else {
                        final AmazonIdentityManagement iam = tenantSession.getIamClient(resolvedTenantId);

                        ListUsersRequest listUsersRequest = ScalityModelConverter.toIAMListUsersRequest(offset, limit);

                        // Add path prefix with osis username to the listusers request if username exists
                        if (resolvedUsername != null && !resolvedUsername.isEmpty()) {
                            listUsersRequest.setPathPrefix("/" + resolvedUsername + "/");
                        }

                        logger.debug("[Vault] List Users Request:{}", new Gson().toJson(listUsersRequest));

                        ListUsersResult listUsersResult = iam.listUsers(listUsersRequest);

                        logger.debug("[Vault] List Users response:{}", new Gson().toJson(listUsersResult));

                        pageOfUsers = ScalityModelConverter.toPageOfUsers(listUsersResult, offset, limit,
                                resolvedTenantId);
                        logger.info("Query Users response:{}", new Gson().toJson(pageOfUsers));
                    }
                    return pageOfUsers;
                });

            } catch (Exception e) {

                logger.warn("Query Users failed; returning empty list: {}", e.getMessage());
                // For errors, Query users should return empty PageOfUsers
                PageInfo pageInfo = new PageInfo(limit, offset);

                PageOfUsers pageOfUsers = new PageOfUsers();
                pageOfUsers.setItems(new ArrayList<>());
                pageOfUsers.setPageInfo(pageInfo);
                return pageOfUsers;
            }
        } else {
            logger.info("Query Users requested with invalid filter; returning empty list");
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
            return tenantSession.run(tenantId, () -> {
                logger.info("Create S3 Credential request received:: tenant ID:{}, user ID:{}",
                        tenantId, userId);

                OsisTenant tenant = ScalityModelConverter
                        .toOsisTenant(vaultAdmin.getAccount(ScalityModelConverter.toGetAccountRequestWithID(tenantId)));
                final AmazonIdentityManagement iamClient = tenantSession.getIamClient(tenantId);

                OsisS3Credential credential = createOsisCredential(tenantId, userId, null, null, iamClient);

                credential.setCdTenantId(tenant.getCdTenantIds().get(0));

                logger.info("Create S3 Credential response:{}, ",
                        ScalityModelConverter.maskSecretKey(new Gson().toJson(credential)));

                return credential;
            });
        } catch (Exception e) {
            // A client-side Vault rejection maps to 400; a genuine Vault server fault (5xx) is
            // preserved so the boundary logs it once at ERROR with the trace.
            throw toResponseException(e);
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
                    logger.info("Query S3 credential: no credential for the given access key; "
                            + "returning empty list: {}", e.getMessage());
                    // For errors, Query Credentials should return empty PageOfS3Credentials
                    return ScalityModelConverter.getEmptyPageOfS3Credentials(offset, limit);
                }
            }
        } else {
            logger.info("Query S3 Credentials requested with invalid filter; returning empty list");
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
            logger.warn("Fail to load S3 capabilities from configuration file {}.", s3CapabilitiesFilePath);
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

            final String resolvedTenantId = tenantId;
            final String resolvedUserId = userId;

            tenantSession.run(resolvedTenantId, () -> {
                final AmazonIdentityManagement iam = tenantSession.getIamClient(resolvedTenantId);

                DeleteAccessKeyRequest deleteAccessKeyRequest = ScalityModelConverter
                        .toDeleteAccessKeyRequest(accessKey, resolvedUserId);

                logger.debug("[Vault] Delete Access Key Request:{}", new Gson().toJson(deleteAccessKeyRequest));

                DeleteAccessKeyResult deleteAccessKeyResult = iam.deleteAccessKey(deleteAccessKeyRequest);

                logger.debug("[Vault] Delete Access Key response:{}", new Gson().toJson(deleteAccessKeyResult));

                secretKeyStore.delete(ScalityModelConverter.toRepoKeyForCredentials(resolvedUserId, accessKey));

                logger.info("Delete S3 credential successful:: tenant ID:{}, user ID:{}, accessKey:{}",
                        resolvedTenantId, resolvedUserId, accessKey);
                return null;
            });

        } catch (Exception e) {
            logger.warn("Delete S3 credential failed; treating as deleted: {}", e.getMessage());
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
            // A genuine Vault server fault (5xx) is preserved so the boundary logs it once at ERROR
            // with the trace. A client-side rejection maps to 400 (errorCode retained for the
            // response contract), keeping the original Vault failure as the cause.
            if (e.getStatus().is5xxServerError()) {
                throw e;
            }
            throw new VaultServiceException(HttpStatus.BAD_REQUEST, e.getErrorCode(), e.getReason(), e);
        }
    }

    @Override
    public void deleteUser(String tenantId, String userId, Boolean purgeData) {
        try {
            tenantSession.run(tenantId, () -> {
                logger.info("Delete User request received:: tenant ID:{}, userID:{}", tenantId, userId);

                final AmazonIdentityManagement iamClient = tenantSession.getIamClient(tenantId);

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
                return null;
            });
        } catch (Exception e) {
            // If delete user fails just return no error response
            logger.warn("Delete User failed; treating as deleted: {}", e.getMessage());
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
            // The S3 Credential doesn't exist for the given access key. Logged once at the boundary.
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

            final String resolvedTenantId = tenantId;
            final String resolvedUserId = userId;

            return tenantSession.run(resolvedTenantId, () -> {
                final AmazonIdentityManagement iam = tenantSession.getIamClient(resolvedTenantId);

                ListAccessKeysRequest listAccessKeysRequest = ScalityModelConverter.toIAMListAccessKeysRequest(
                        resolvedUserId, limit);

                logger.debug("[Vault] List Access Keys Request:{}", new Gson().toJson(listAccessKeysRequest));

                ListAccessKeysResult listAccessKeysResult = iam.listAccessKeys(listAccessKeysRequest);

                logger.debug("[Vault] List Access Keys response:{}", new Gson().toJson(listAccessKeysResult));

                Optional<AccessKeyMetadata> accessKeyResult = listAccessKeysResult.getAccessKeyMetadata()
                        .stream()
                        .filter(accessKeyMetadata -> accessKeyMetadata.getAccessKeyId().equals(accessKey))
                        .findAny();

                if (accessKeyResult.isPresent()) {
                    AccessKeyMetadata accessKeyMetadata = accessKeyResult.get();

                    String secretKey = secretKeyStore.retrieve(
                            ScalityModelConverter.toRepoKeyForCredentials(resolvedUserId,
                                    accessKeyMetadata.getAccessKeyId()));

                    // get Osis User by userId
                    GetUserRequest getUserRequest = ScalityModelConverter.toIAMGetUserRequest(resolvedUserId);
                    logger.debug("[Vault] Get User Request:{}", new Gson().toJson(getUserRequest));
                    GetUserResult getUserResult = iam.getUser(getUserRequest);
                    logger.debug("[Vault] Get User response:{}", new Gson().toJson(getUserResult));
                    OsisUser osisUser = ScalityModelConverter.toOsisUser(getUserResult.getUser(), resolvedTenantId);

                    OsisS3Credential osisCredential = ScalityModelConverter.toOsisS3Credentials(resolvedTenantId,
                            osisUser.getCdTenantId(),
                            accessKeyMetadata,
                            secretKey);
                    logger.info("Get S3 credential  response:{}",
                            ScalityModelConverter.maskSecretKey(new Gson().toJson(osisCredential)));

                    return osisCredential;
                } else {
                    throw new NotFoundException("The S3 Credential doesn't exist for the given access key");
                }
            });

        } catch (Exception e) {
            // The S3 Credential doesn't exist for the given access key. Logged once at the boundary.
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
            // The tenant doesn't exist. Logged once at the boundary.
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
            final AmazonIdentityManagement iamClient = tenantSession.getIamClient(account.getId());

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
            // The tenant doesn't exist. Logged once at the boundary.
            throw new VaultServiceException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @Override
    public OsisUser getUser(String tenantId, String userId) {
        try {
            return tenantSession.run(tenantId, () -> {
                logger.info("Get User request received:: tenant ID:{}, userID:{}", tenantId, userId);

                final AmazonIdentityManagement iamClient = tenantSession.getIamClient(tenantId);

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
            });
        } catch (Exception e) {
            // User not found. Logged once at the boundary.
            throw new VaultServiceException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }

    }

    @Override
    public void headTenant(String tenantId) {
        try {
            logger.info("Head Tenant request received:: tenant ID:{}", tenantId);
            // OSE can send a cloud director tenant UUID as tenant ID
            // as a part of their flow instead of the storage account ID
            if (ScalityUtils.isValidUUID(tenantId)) {
                throw new NotFoundException("Invalid Tenant ID");
            }
            AccountData accountData = vaultAdmin.getAccount(ScalityModelConverter.toGetAccountRequestWithID(tenantId));
            logger.info("Head Tenant response:: {}", accountData);
            if (accountData == null || !accountData.getId().equals(tenantId)) {
                throw new NotFoundException("The tenant does not exist for the given ID");
            }
        } catch (Exception e) {
            // ideally we should catch the NotFoundException separately and throw it
            // and for other errors a generic exception should be thrown such as RuntimeException
            // Post testing with Vmware OSE 2.2.0.1, OSE expects a 404 in any error scenario and does not handle any other error code
            // Reference: https://developer.vmware.com/apis/1034#/tenant/headTenant
            // Logged once at the boundary.
            throw new NotFoundException("Head Tenant error. Error details: " + e.getMessage());
        }
    }

    @Override
    public boolean headUser(String tenantId, String userId) {
        try {
            logger.info("Head User request received:: tenant ID:{} user ID:{}", tenantId, userId);
            final AmazonIdentityManagement iamClient = tenantSession.getIamClient(tenantId);

            GetUserRequest getUserRequest = ScalityModelConverter.toIAMGetUserRequest(userId);

            logger.debug("[Vault] Get User Request:{}", new Gson().toJson(getUserRequest));

            GetUserResult getUserResult = iamClient.getUser(getUserRequest);
            logger.info("Head User response:: {}", getUserResult.getUser());
            return getUserResult.getUser() != null && getUserResult.getUser().getUserName().equals(userId);
        } catch (Exception e) {
            // Logged once at the boundary.
            throw new VaultServiceException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @Override
    public PageOfS3Credentials listS3Credentials(String tenantId, String userId, Long offset, Long limit) {
        try {
            return tenantSession.run(tenantId, () -> {
                OsisTenant tenant = ScalityModelConverter
                        .toOsisTenant(vaultAdmin.getAccount(ScalityModelConverter.toGetAccountRequestWithID(tenantId)));
                logger.info("List s3 credentials request received:: tenant ID:{}, user ID:{}, offset:{}, limit:{}",
                        tenantId, userId, offset, limit);

                final AmazonIdentityManagement iam = tenantSession.getIamClient(tenantId);

                ListAccessKeysRequest listAccessKeysRequest = ScalityModelConverter.toIAMListAccessKeysRequest(userId,
                        limit);

                logger.debug("[Vault] List Access Keys Request:{}", new Gson().toJson(listAccessKeysRequest));

                ListAccessKeysResult listAccessKeysResult = iam.listAccessKeys(listAccessKeysRequest);

                logger.debug("[Vault] List Access Keys response:{}", new Gson().toJson(listAccessKeysResult));

                Map<String, String> secretKeyMap = new HashMap<>();
                for (AccessKeyMetadata accessKey : listAccessKeysResult.getAccessKeyMetadata()) {
                    String secretKey = secretKeyStore.retrieve(
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
            });
        } catch (Exception e) {
            logger.warn("List S3 Credentials failed; returning empty list: {}", e.getMessage());
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

            final String resolvedTenantId = tenantIdOfCurrentUser;
            final String resolvedUserId = userIdOfCurrentUser;

            return tenantSession.run(resolvedTenantId, () -> {
                final AmazonIdentityManagement iam = tenantSession.getIamClient(resolvedTenantId);
                UpdateAccessKeyRequest updateAccessKeyRequest = ScalityModelConverter.toIAMUpdateAccessKeyRequest(
                        resolvedUserId,
                        accessKey,
                        osisS3Credential.getActive());
                iam.updateAccessKey(updateAccessKeyRequest);
                OsisS3Credential newOsisS3Credential = this.getS3Credential(resolvedTenantId, resolvedUserId, accessKey);
                logger.info("UpdatedCredentialStatus response:{}", ScalityModelConverter.maskSecretKey(new Gson().toJson(newOsisS3Credential)));
                return newOsisS3Credential;
            });
        } catch (Exception e) {
            // A client-side Vault rejection maps to 400; a genuine Vault server fault (5xx) is
            // preserved so the boundary logs it once at ERROR with the trace.
            throw toResponseException(e);
        }
    }

    @Override
    public PageOfUsers listUsers(String tenantId, long offset, long limit) {
        try {
            return tenantSession.run(tenantId, () -> {
                logger.info("List Users request received:: tenant ID:{}, offset:{}, limit:{}", tenantId, offset, limit);

                final AmazonIdentityManagement iam = tenantSession.getIamClient(tenantId);

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
            });
        } catch (Exception e) {
            logger.warn("List Users failed; returning empty list: {}", e.getMessage());
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
            return tenantSession.run(tenantId, () -> {
                OsisTenant tenant = ScalityModelConverter
                        .toOsisTenant(vaultAdmin.getAccount(ScalityModelConverter.toGetAccountRequestWithID(tenantId)));
                logger.info("Update User request received:: tenant ID:{}, user ID:{}", tenantId, userId);

                final AmazonIdentityManagement iam = tenantSession.getIamClient(tenantId);

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

                logger.info("Updated user response:{}",
                        ScalityModelConverter.maskSecretKey(new Gson().toJson(osisUser)));
                return osisUser;
            });
        } catch (Exception e) {
            // A client-side Vault rejection maps to 400; a genuine Vault server fault (5xx) is
            // preserved so the boundary logs it once at ERROR with the trace.
            throw toResponseException(e);
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
            return tenantSession.run(tenantId, () -> {
                logger.info("Get bucket list request received:: tenant ID:{}, offset:{}, limit:{}", tenantId, offset, limit);
                final AmazonS3 s3Client = tenantSession.getS3Client(tenantId);

                // Get account info by tenant ID for canonical ID
                GetAccountRequestDTO getAccountRequest = ScalityModelConverter.toGetAccountRequestWithID(tenantId);
                logger.debug("[Vault]GetAccount request:{}", new Gson().toJson(getAccountRequest));

                AccountData accountData = vaultAdmin.getAccount(getAccountRequest);
                logger.debug("[Vault]GetAccount response:{}", new Gson().toJson(accountData));

                //s3 listBucket has no pagination, so list all
                List<Bucket> buckets = s3Client.listBuckets();
                logger.debug("[S3] List all Buckets size:{}", buckets.size());

                PageOfOsisBucketMeta pageOfOsisBucketMeta = ScalityModelConverter.toPageOfOsisBucketMeta(
                        buckets, accountData.getCanonicalId(), offset, limit);
                logger.info("List Buckets response:{}", new Gson().toJson(pageOfOsisBucketMeta));

                return pageOfOsisBucketMeta;
            });
        } catch (Exception e) {
            logger.warn("Get Bucket List failed; returning empty list: {}", e.getMessage());
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
        throw new NotImplementedException();
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
     * Create osis credential osis s 3 credential.
     *
     * @param tenantId   the tenant id
     * @param userId     the user id
     * @param cdTenantId the cd tenant id
     * @param username   the username
     * @param iam        the iam
     * @return the osis s 3 credential
     */
    OsisS3Credential createOsisCredential(String tenantId, String userId, String cdTenantId, String username,
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

        secretKeyStore.store(
                ScalityModelConverter.toRepoKeyForCredentials(userId,
                        createAccessKeyResult.getAccessKey().getAccessKeyId()),
                createAccessKeyResult.getAccessKey().getSecretAccessKey());

        return createAccessKeyResult;
    }

    Policy getOrCreateUserPolicy(AmazonIdentityManagement iam, String tenantId) {
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
