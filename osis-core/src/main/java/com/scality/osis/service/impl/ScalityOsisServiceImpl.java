/**
 * Copyright 2020 VMware, Inc.
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.service.impl;

import com.amazonaws.services.identitymanagement.*;
import com.amazonaws.services.identitymanagement.model.*;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.amazonaws.util.StringUtils;
import com.scality.osis.ScalityAppEnv;
import com.scality.osis.redis.service.IRedisRepository;
import com.scality.osis.service.ScalityOsisService;
import com.scality.osis.security.utils.CipherFactory;
import com.scality.osis.utils.ScalityUtils;
import com.google.gson.Gson;
import com.scality.osis.utils.ScalityModelConverter;
import com.scality.osis.vaultadmin.VaultAdmin;
import com.scality.osis.vaultadmin.impl.VaultServiceException;
import com.scality.vaultclient.dto.AccountData;
import com.scality.vaultclient.dto.CreateAccountRequestDTO;
import com.scality.vaultclient.dto.CreateAccountResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scality.vaultclient.dto.GetAccountRequestDTO;
import com.scality.vaultclient.dto.ListAccountsRequestDTO;
import com.scality.vaultclient.dto.ListAccountsResponseDTO;
import com.vmware.osis.model.*;
import com.vmware.osis.model.exception.NotFoundException;
import com.vmware.osis.model.exception.NotImplementedException;
import com.vmware.osis.resource.OsisCapsManager;
import com.scality.osis.security.crypto.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.scality.osis.utils.ScalityConstants.*;


/**
 * The type Scality osis service.
 */
@Service
@Primary
public class ScalityOsisServiceImpl implements ScalityOsisService {
    private static final Logger logger = LoggerFactory.getLogger(ScalityOsisServiceImpl.class);
    private static final String S3_CAPABILITIES_JSON = "s3capabilities.json";

    @Autowired
    private ScalityAppEnv appEnv;

    @Autowired
    private VaultAdmin vaultAdmin;

    @Autowired
    private OsisCapsManager osisCapsManager;

    @Autowired
    private AsyncScalityOsisService asyncScalityOsisService;

    @Autowired
    private IRedisRepository<SecretKeyRepoData> scalityRedisRepository;

    @Autowired
    private CipherFactory cipherFactory;

    private final Map<String, SecretKeyRepoData> springLocalCache = new ConcurrentHashMap<>();

    /**
     * Instantiates a new Scality osis service.
     */
    public ScalityOsisServiceImpl(){}

    /**
     * Instantiates a new Scality osis service.
     *
     * @param appEnv          the app env
     * @param vaultAdmin      the vault admin
     * @param osisCapsManager the osis caps manager
     */
    public ScalityOsisServiceImpl(ScalityAppEnv appEnv, VaultAdmin vaultAdmin, OsisCapsManager osisCapsManager){
        this.appEnv = appEnv;
        this.vaultAdmin = vaultAdmin;
        this.osisCapsManager = osisCapsManager;
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
        } catch (VaultServiceException e){
            // Create Tenant supports only 400:BAD_REQUEST error, change status code in the VaultServiceException
            logger.error("Create Tenant error. Error details: ", e);
            throw new VaultServiceException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @Override
    public PageOfTenants queryTenants(long offset, long limit, String filter) {
        if(filter.contains(CD_TENANT_ID_PREFIX)) {
            try {
                logger.info("Query Tenants request received: offset={}, limit={}, filter ={}", offset, limit, filter);

                String cdTenantId = ScalityModelConverter.extractCdTenantId(filter);

                PageOfTenants pageOfTenants = null;
                if(ScalityUtils.isValidUUID(cdTenantId)) {

                    ListAccountsRequestDTO listAccountsRequest = ScalityModelConverter.toScalityListAccountsRequest(limit, filter);

                    logger.debug("[Vault] List Accounts Request:{}", new Gson().toJson(listAccountsRequest));
                    ListAccountsResponseDTO listAccountsResponseDTO = vaultAdmin.listAccounts(offset, listAccountsRequest);

                    logger.debug("[Vault] List Accounts response:{}", new Gson().toJson(listAccountsResponseDTO));

                    pageOfTenants = ScalityModelConverter.toPageOfTenants(listAccountsResponseDTO, offset, limit);
                } else {
                    GetAccountRequestDTO getAccountRequest = ScalityModelConverter.toGetAccountRequestWithID(cdTenantId);

                    logger.debug("[Vault] Get Account Request:{}", new Gson().toJson(getAccountRequest));

                    AccountData account = vaultAdmin.getAccount(getAccountRequest);
                    pageOfTenants = ScalityModelConverter.toPageOfTenants(account, offset, limit);
                }

                logger.info("Query Tenants response:{}", new Gson().toJson(pageOfTenants));

                return pageOfTenants;

            } catch (VaultServiceException e) {
                logger.error("Query Tenants error. Return empty list. Error details: ", e);
                // For errors, List Tenants should return empty PageOfTenants
                PageInfo pageInfo = new PageInfo();
                pageInfo.setLimit(limit);
                pageInfo.setOffset(offset);
                pageInfo.setTotal(0L);

                PageOfTenants pageOfTenants = new PageOfTenants();
                pageOfTenants.setItems(new ArrayList<>());
                pageOfTenants.setPageInfo(pageInfo);
                return pageOfTenants;
            }
        } else {
            // Returning all the tenants with given offset and limit as filter is not with `cd_tenant_id`
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

        } catch (VaultServiceException e){
            logger.error("List Tenants error. Return empty list. Error details: ", e);
            // For errors, List Tenants should return empty PageOfTenants
            PageInfo pageInfo = new PageInfo();
            pageInfo.setLimit(limit);
            pageInfo.setOffset(offset);
            pageInfo.setTotal(0L);

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

            AccountData accountData = vaultAdmin.getAccount(ScalityModelConverter.toGetAccountRequestWithID(osisUser.getTenantId()));
            osisUser.setCanonicalUserId(accountData.getCanonicalId());

            Credentials tempCredentials = getCredentials(osisUser.getTenantId());
            final AmazonIdentityManagement iam = vaultAdmin.getIAMClient(tempCredentials, appEnv.getRegionInfo().get(0));

            CreateUserRequest createUserRequest =  ScalityModelConverter.toCreateUserRequest(osisUser);
            logger.debug("[Vault] Create User Request:{}", new Gson().toJson(createUserRequest));

            CreateUserResult createUserResult = iam.createUser(createUserRequest);

            logger.debug("[Vault] Create User response:{}", new Gson().toJson(createUserResult));

            OsisUser resOsisUser = null;

            if( null != createUserResult) {

                resOsisUser = ScalityModelConverter.toOsisUser(createUserResult, osisUser.getTenantId());

                /** Get userpolicy@<Account_id> **/
                Policy userPolicy = getOrCreateUserPolicy(iam, resOsisUser.getTenantId());

                /** Attach user policy to the user **/
                AttachUserPolicyRequest attachUserPolicyRequest = ScalityModelConverter.toAttachUserPolicyRequest(userPolicy.getArn(), resOsisUser.getUserId());
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

                logger.info("Create User response:{}", ScalityModelConverter.maskSecretKey(new Gson().toJson(resOsisUser)));

            }

            return resOsisUser;

        } catch (Exception e){
            if(isAdminPolicyError(e) && !StringUtils.isNullOrEmpty(osisUser.getTenantId())){
                try {
                    generateAdminPolicy(osisUser.getTenantId());
                    return createUser(osisUser);
                } catch (Exception ex) {
                    e = ex;
                }
            }
            // Create User supports only 400:BAD_REQUEST error, change status code in the VaultServiceException
            logger.error("Create User error. Error details: ", e);
            throw new VaultServiceException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }

    }

    @Override
    public PageOfUsers queryUsers(long offset, long limit, String filter) {
        if(filter.contains(CD_TENANT_ID_PREFIX) && filter.contains(DISPLAY_NAME_PREFIX)) {
            String tenantId = null;
            try {
                logger.info("Query Users request received:: filter:{}, offset:{}, limit:{}", filter, offset, limit);

                String osisUserName = ScalityModelConverter.extractOsisUserName(filter);

                String cdTenantIdFilter = ScalityModelConverter.extractCdTenantIdFilter(filter);

                String cdTenantId = ScalityModelConverter.extractCdTenantId(cdTenantIdFilter);

                if(ScalityUtils.isValidUUID(cdTenantId)) {
                    ListAccountsRequestDTO queryAccountsRequest = ScalityModelConverter.toScalityListAccountsRequest(limit, cdTenantIdFilter);

                    tenantId = vaultAdmin.getAccountID(queryAccountsRequest);
                } else {
                    tenantId = cdTenantId;
                }

                Credentials tempCredentials = getCredentials(tenantId);
                final AmazonIdentityManagement iam = vaultAdmin.getIAMClient(tempCredentials, appEnv.getRegionInfo().get(0));

                ListUsersRequest listUsersRequest =  ScalityModelConverter.toIAMListUsersRequest(offset, limit);

                // Add path prefix with osis username to the listusers request
                listUsersRequest.setPathPrefix("/" + osisUserName + "/");

                logger.debug("[Vault] List Users Request with 'pathPrefix':{}", new Gson().toJson(listUsersRequest));

                ListUsersResult listUsersResult = iam.listUsers(listUsersRequest);

                logger.debug("[Vault] List Users response:{}", new Gson().toJson(listUsersResult));

                PageOfUsers pageOfUsers = ScalityModelConverter.toPageOfUsers(listUsersResult, offset, limit, tenantId);
                logger.info("Query Users response:{}", new Gson().toJson(pageOfUsers));

                return pageOfUsers;

            } catch (Exception e) {

                if(isAdminPolicyError(e) && !StringUtils.isNullOrEmpty(tenantId)){
                    try {
                        generateAdminPolicy(tenantId);
                        return queryUsers(offset, limit, filter);
                    } catch (Exception ex) {
                        e = ex;
                    }
                }

                logger.error("Query Users error. Return empty list. Error details: ", e);
                // For errors, Query users should return empty PageOfUsers
                PageInfo pageInfo = new PageInfo();
                pageInfo.setLimit(limit);
                pageInfo.setOffset(offset);
                pageInfo.setTotal(0L);

                PageOfUsers pageOfUsers = new PageOfUsers();
                pageOfUsers.setItems(new ArrayList<>());
                pageOfUsers.setPageInfo(pageInfo);
                return pageOfUsers;
            }
        } else {
            logger.error("QueryUsers requested with invalid filter. Returns empty set of users");
            // For errors, Query Users should return empty PageOfUsers
            PageInfo pageInfo = new PageInfo();
            pageInfo.setLimit(limit);
            pageInfo.setOffset(offset);
            pageInfo.setTotal(0L);

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

            Credentials tempCredentials = getCredentials(tenantId);
            final AmazonIdentityManagement iamClient = vaultAdmin.getIAMClient(tempCredentials, appEnv.getRegionInfo().get(0));

            OsisS3Credential credential =  createOsisCredential(tenantId, userId, null, null, iamClient);

            logger.info("Create S3 Credential response:{}, ", ScalityModelConverter.maskSecretKey(new Gson().toJson(credential)));

            return credential;
        } catch (Exception e){
            if(isAdminPolicyError(e)){
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
        if(filter.contains(TENANT_ID_PREFIX) && filter.contains(USER_ID_PREFIX)) {
            Map<String, String> kvMap = ScalityUtils.parseFilter(filter);
            String tenantId = kvMap.get(OSIS_TENANT_ID);
            String userId = kvMap.get(OSIS_USER_ID);
            String accessKey = kvMap.get(OSIS_ACCESS_KEY);

            if(StringUtils.isNullOrEmpty(accessKey)) {
                return listS3Credentials(tenantId, userId, offset, limit);
            } else {

                try {
                    return ScalityModelConverter.toPageOfS3Credentials(
                                                                        getS3Credential(tenantId, userId, accessKey, limit),
                                                                        offset,
                                                                        limit);

                } catch (Exception e) {
                    logger.error("Query S3 credential :: The S3 Credential doesn't exist for the given access key. Error details:", e);
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
        OsisS3Capabilities osisS3Capabilities = new OsisS3Capabilities();
        try {
            osisS3Capabilities = new ObjectMapper()
                    .readValue(new ClassPathResource(S3_CAPABILITIES_JSON).getInputStream(),
                            OsisS3Capabilities.class);
        } catch (IOException e) {
            logger.info("Fail to load S3 capabilities from configuration file {}.", S3_CAPABILITIES_JSON);
        }
        return osisS3Capabilities;
    }

    @Override
    public void deleteS3Credential(String tenantId, String userId, String accessKey) {
        throw new NotImplementedException();
    }

    @Override
    public void deleteTenant(String tenantId, Boolean purgeData) {
        throw new NotImplementedException();
    }

    @Override
    public OsisTenant updateTenant(String tenantId, OsisTenant osisTenant) {
        throw new NotImplementedException();
    }

    @Override
    public void deleteUser(String tenantId, String userId, Boolean purgeData) {
        throw new NotImplementedException();
    }

    @Override
    public OsisS3Credential getS3Credential(String accessKey) {
        throw new NotImplementedException();
    }

    @Override
    public OsisS3Credential getS3Credential(String tenantId, String userId, String accessKey) {
        return getS3Credential(tenantId, userId, accessKey, DEFAULT_MAX_LIMIT);
    }

    private OsisS3Credential getS3Credential(String tenantId, String userId, String accessKey, long limit) {

        if(!StringUtils.isNullOrEmpty(tenantId) && !StringUtils.isNullOrEmpty(userId)) {
            try {
                logger.info("Get s3 credential request received:: tenant ID:{}, user ID:{}, accessKey:{}",
                        tenantId, userId, accessKey);

                Credentials tempCredentials = getCredentials(tenantId);
                final AmazonIdentityManagement iam = vaultAdmin.getIAMClient(tempCredentials, appEnv.getRegionInfo().get(0));

                ListAccessKeysRequest listAccessKeysRequest =  ScalityModelConverter.toIAMListAccessKeysRequest(userId, limit);

                logger.debug("[Vault] List Access Keys Request:{}", new Gson().toJson(listAccessKeysRequest));

                ListAccessKeysResult listAccessKeysResult = iam.listAccessKeys(listAccessKeysRequest);

                logger.debug("[Vault] List Access Keys response:{}", new Gson().toJson(listAccessKeysResult));

                Optional<AccessKeyMetadata> accessKeyResult = listAccessKeysResult.getAccessKeyMetadata()
                        .stream()
                        .filter(accessKeyMetadata -> accessKeyMetadata.getAccessKeyId().equals(accessKey))
                        .findAny();

                if(accessKeyResult.isPresent()) {
                    AccessKeyMetadata accessKeyMetadata = accessKeyResult.get();

                    String secretKey = retrieveSecretKey(ScalityModelConverter.toRepoKeyForCredentials(userId, accessKeyMetadata.getAccessKeyId()));

                    OsisS3Credential osisCredential = ScalityModelConverter.toOsisS3Credentials(tenantId,
                            accessKeyMetadata,
                            secretKey);
                    logger.info("Get S3 credential  response:{}", ScalityModelConverter.maskSecretKey(new Gson().toJson(osisCredential)));

                    return  osisCredential;
                } else {
                    throw new NotFoundException("The S3 Credential doesn't exist for the given access key");
                }

            } catch (Exception e){

                if(isAdminPolicyError(e)) {
                    try {
                        generateAdminPolicy(tenantId);
                        return getS3Credential(tenantId, userId, accessKey);
                    } catch (Exception ex) {
                        e = ex;
                    }
                }
                logger.error("Get S3 credential :: The S3 Credential doesn't exist for the given access key. Error details:", e);
                throw new VaultServiceException(HttpStatus.NOT_FOUND, e.getMessage(), e);
            }
        } else {
            throw new VaultServiceException(HttpStatus.NOT_FOUND, "TenantID and UserID are mandatory for this platform");
        }
    }

    @Override
    public OsisTenant getTenant(String tenantId) {
        throw new NotImplementedException();
    }

    @Override
    public OsisUser getUser(String canonicalUserId) {
        try {
            logger.info("Get User w/ Canonical ID request received:: canonicalUserId ID:{}", canonicalUserId);

            GetAccountRequestDTO getAccountRequest = ScalityModelConverter.toGetAccountRequestWithCanonicalID(canonicalUserId);

            logger.debug("[Vault] Get Account Request:{}", new Gson().toJson(getAccountRequest));

            AccountData account = vaultAdmin.getAccount(getAccountRequest);

            logger.debug("[Vault] Get Account response:{}", new Gson().toJson(account));

            List<OsisUser> users = listUsers(account.getId(), DEFAULT_MIN_OFFSET, DEFAULT_MAX_LIMIT).getItems();

            final OsisUser osisUser = ScalityModelConverter.toCanonicalOsisUser(account, users);

            logger.info("Get User w/ Canonical ID response:{}", new Gson().toJson(osisUser));

            return osisUser;
        } catch (Exception e){
            logger.error("The tenant doesn't exist. Error details: ", e);
            throw new VaultServiceException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @Override
    public OsisUser getUser(String tenantId, String userId) {
        try {
            logger.info("Get User request received:: tenant ID:{}, userID:{}", tenantId, userId);

            Credentials tempCredentials = getCredentials(tenantId);
            final AmazonIdentityManagement iamClient = vaultAdmin.getIAMClient(tempCredentials, appEnv.getRegionInfo().get(0));

            GetUserRequest getUserRequest =  ScalityModelConverter.toIAMGetUserRequest(userId);

            logger.debug("[Vault] Get User Request:{}", new Gson().toJson(getUserRequest));

            GetUserResult getUserResult = iamClient.getUser(getUserRequest);

            logger.debug("[Vault] Get User response:{}", new Gson().toJson(getUserResult));

            OsisUser osisUser = ScalityModelConverter.toOsisUser(getUserResult.getUser(), tenantId);
            logger.info("Get User response:{}", new Gson().toJson(osisUser));

            return  osisUser;
        } catch (Exception e){

            if(isAdminPolicyError(e)){
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
        throw new NotImplementedException();
    }

    @Override
    public boolean headUser(String tenantId, String userId) {
        throw new NotImplementedException();
    }

    @Override
    public PageOfS3Credentials listS3Credentials(String tenantId, String userId, Long offset, Long limit) {
        try {
            logger.info("List s3 credentials request received:: tenant ID:{}, user ID:{}, offset:{}, limit:{}",
                    tenantId, userId, offset, limit);

            Credentials tempCredentials = getCredentials(tenantId);
            final AmazonIdentityManagement iam = vaultAdmin.getIAMClient(tempCredentials, appEnv.getRegionInfo().get(0));

            ListAccessKeysRequest listAccessKeysRequest =  ScalityModelConverter.toIAMListAccessKeysRequest(userId, limit);

            logger.debug("[Vault] List Access Keys Request:{}", new Gson().toJson(listAccessKeysRequest));

            ListAccessKeysResult listAccessKeysResult = iam.listAccessKeys(listAccessKeysRequest);

            logger.debug("[Vault] List Access Keys response:{}", new Gson().toJson(listAccessKeysResult));

            Map<String, String> secretKeyMap = new HashMap<>();
            for(AccessKeyMetadata accessKey: listAccessKeysResult.getAccessKeyMetadata()) {
                String secretKey = retrieveSecretKey(ScalityModelConverter.toRepoKeyForCredentials(userId, accessKey.getAccessKeyId()));
                if(!StringUtils.isNullOrEmpty(secretKey)){
                    secretKeyMap.put(accessKey.getAccessKeyId(), secretKey);
                }
            }

            // If no secret keys are present in Redis, create a new key and add it to secretKeyMap
            if(secretKeyMap.isEmpty()) {
                CreateAccessKeyResult createAccessKeyResult = createAccessKey(userId, iam);

                AccessKeyMetadata newAccessKeyMetadata = ScalityModelConverter.toAccessKeyMetadata(createAccessKeyResult.getAccessKey());
                listAccessKeysResult.getAccessKeyMetadata().add(newAccessKeyMetadata);

                secretKeyMap.put(createAccessKeyResult.getAccessKey().getAccessKeyId(), createAccessKeyResult.getAccessKey().getSecretAccessKey());
            }

            PageOfS3Credentials pageOfS3Credentials = ScalityModelConverter
                    .toPageOfS3Credentials(listAccessKeysResult, offset, limit, tenantId, secretKeyMap);
            logger.info("List S3 credentials  response:{}", ScalityModelConverter.maskSecretKey(new Gson().toJson(pageOfS3Credentials)));

            return  pageOfS3Credentials;
        } catch (Exception e){

            if(isAdminPolicyError(e)) {
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
    public PageOfUsers listUsers(String tenantId, long offset, long limit) {
        try {
            logger.info("List Users request received:: tenant ID:{}, offset:{}, limit:{}", tenantId, offset, limit);

            Credentials tempCredentials = getCredentials(tenantId);
            final AmazonIdentityManagement iam = vaultAdmin.getIAMClient(tempCredentials, appEnv.getRegionInfo().get(0));

            ListUsersRequest listUsersRequest =  ScalityModelConverter.toIAMListUsersRequest(offset, limit);

            logger.debug("[Vault] List Users Request:{}", new Gson().toJson(listUsersRequest));

            ListUsersResult listUsersResult = iam.listUsers(listUsersRequest);

            logger.debug("[Vault] List Users response:{}", new Gson().toJson(listUsersResult));

            PageOfUsers pageOfUsers = ScalityModelConverter.toPageOfUsers(listUsersResult, offset, limit, tenantId);
            logger.info("List Users response:{}", new Gson().toJson(pageOfUsers));

            return  pageOfUsers;
        } catch (Exception e){

            if(isAdminPolicyError(e)) {
                try {
                    generateAdminPolicy(tenantId);
                    return listUsers(tenantId, offset, limit);
                } catch (Exception ex) {
                    e = ex;
                }
            }

            logger.error("ListUsers error. Returning empty list. Error details: ", e);
            // For errors, List Users should return empty PageOfUsers
            PageInfo pageInfo = new PageInfo();
            pageInfo.setLimit(limit);
            pageInfo.setOffset(offset);
            pageInfo.setTotal(0L);

            PageOfUsers pageOfUsers = new PageOfUsers();
            pageOfUsers.setItems(new ArrayList<>());
            pageOfUsers.setPageInfo(pageInfo);
            return pageOfUsers;
        }

    }

    @Override
    public OsisUser updateUser(String tenantId, String userId, OsisUser osisUser) {
        throw new NotImplementedException();
    }

    @Override
    public Information getInformation(String domain) {
        return new Information()
                .addAuthModesItem(appEnv.isApiTokenEnabled() ? Information.AuthModesEnum.BEARER : Information.AuthModesEnum.BASIC)
                .storageClasses(appEnv.getStorageInfo())
                .regions(appEnv.getRegionInfo())
                .platformName(appEnv.getPlatformName())
                .platformVersion(appEnv.getPlatformVersion())
                .apiVersion(appEnv.getApiVersion())
                .notImplemented(osisCapsManager.getNotImplements())
                .logoUri(ScalityUtils.getLogoUri(domain))
                .services(new InformationServices().iam(domain + IAM_PREFIX).s3(appEnv.getS3InterfaceEndpoint()))
                .status(Information.StatusEnum.NORMAL);
    }

    @Override
    public OsisCaps updateOsisCaps(OsisCaps osisCaps) {
        throw new NotImplementedException();
    }

    @Override
    public PageOfOsisBucketMeta getBucketList(String tenantId, long offset, long limit) {
        throw new NotImplementedException();
    }

    @Override
    public OsisUsage getOsisUsage(Optional<String> tenantId, Optional<String> userId) {
        return new OsisUsage();
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
            AssumeRoleRequest assumeRoleRequest = ScalityModelConverter.getAssumeRoleRequestForAccount(accountID, appEnv.getAssumeRoleName());
            logger.debug("[Vault] Assume Role request:{}", assumeRoleRequest);
            credentials = vaultAdmin.getTempAccountCredentials(assumeRoleRequest);
            logger.debug("[Vault] Assume Role response received with access key:{}, expiration:{}", credentials.getAccessKeyId(), credentials.getExpiration());
        } catch(VaultServiceException e) {

            if(!StringUtils.isNullOrEmpty(e.getErrorCode()) &&
                    NO_SUCH_ENTITY_ERR.equals(e.getErrorCode()) &&
                    ROLE_DOES_NOT_EXIST_ERR.equals(e.getReason())){
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
    public OsisS3Credential createOsisCredential(String tenantId, String userId, String cdTenantId, String username, AmazonIdentityManagement iam) throws Exception {

        CreateAccessKeyResult createAccessKeyResult =  createAccessKey(userId, iam);

        return ScalityModelConverter.toOsisS3Credentials(cdTenantId,
                tenantId,
                username,
                createAccessKeyResult);
    }

    /**
     * Create access key on iam.
     *
     * @param userId     the user id
     * @param iam        the iam
     * @return the iam access key
     */
    private CreateAccessKeyResult createAccessKey(String userId, AmazonIdentityManagement iam) throws Exception {

        CreateAccessKeyRequest createAccessKeyRequest =  ScalityModelConverter.toCreateUserAccessKeyRequest(userId);

        logger.debug("[Vault] Create User Access Key Request:{}", new Gson().toJson(createAccessKeyRequest));

        CreateAccessKeyResult createAccessKeyResult = iam.createAccessKey(createAccessKeyRequest);

        logger.debug("[Vault] Create User Access Key Response:{}", createAccessKeyResult);

        storeSecretKey(
                ScalityModelConverter.toRepoKeyForCredentials(userId, createAccessKeyResult.getAccessKey().getAccessKeyId()),
                createAccessKeyResult.getAccessKey().getSecretAccessKey());

        return createAccessKeyResult;
    }

    public Policy getOrCreateUserPolicy(AmazonIdentityManagement iam, String tenantId) {
        Policy userPolicy = null;

        GetPolicyRequest getPolicyRequest = ScalityModelConverter.toGetPolicyRequest(tenantId);

        logger.debug("[Vault] Get Policy Request:{}", new Gson().toJson(getPolicyRequest));

        try {
            GetPolicyResult getPolicyResult = iam.getPolicy(getPolicyRequest);

            logger.debug("[Vault] Get Policy response:{}", new Gson().toJson(getPolicyResult));

            userPolicy = getPolicyResult.getPolicy();

        } catch(com.amazonaws.services.identitymanagement.model.NoSuchEntityException e){
            /** Policy does not exists **/
            logger.debug("[Vault] User policy does not exists. A new user policy will be created");
        }

        if(userPolicy == null || StringUtils.isNullOrEmpty(userPolicy.getArn())){
            /** Create a new policy with necessary permissions **/
            CreatePolicyRequest createPolicyRequest = ScalityModelConverter.toCreateUserPolicyRequest(tenantId);
            logger.debug("[Vault] Create Policy Request:{}", new Gson().toJson(createPolicyRequest));

            CreatePolicyResult createPolicyResult = iam.createPolicy(createPolicyRequest);
            logger.debug("[Vault] Create Policy response:{}", new Gson().toJson(createPolicyResult));

            userPolicy = createPolicyResult.getPolicy();

        }
        return userPolicy;
    }

    private void generateAdminPolicy(String tenantId) throws Exception {
        AccountData account = vaultAdmin.getAccount(ScalityModelConverter.toGetAccountRequestWithID(tenantId));
        asyncScalityOsisService.setupAdminPolicy(tenantId, account.getName(), appEnv.getAssumeRoleName());
    }

    private boolean isAdminPolicyError(Exception e) {
        return e instanceof AmazonIdentityManagementException &&
                (HttpStatus.FORBIDDEN.value() == ((AmazonIdentityManagementException) e).getStatusCode());
    }

    private void storeSecretKey(String repoKey, String secretAccessKey) throws Exception {
        // Using `repoKey` for Associated Data during encryption
        SecretKeyRepoData encryptedRepoData = cipherFactory.getCipher().encrypt(secretAccessKey,
                                                                cipherFactory.getLatestSecretCipherKey(),
                                                                repoKey);

        encryptedRepoData.setKeyID(cipherFactory.getLatestCipherID());
        encryptedRepoData.getCipherInfo().setCipherName(cipherFactory.getLatestCipherName());
        // Prefix Cipher ID to the encrypted value

        if(REDIS_SPRING_CACHE_TYPE.equalsIgnoreCase(appEnv.getSpringCacheType())) {
            scalityRedisRepository.save(repoKey, encryptedRepoData);
        } else {
            springLocalCache.put(repoKey, encryptedRepoData);
        }
    }

    private String retrieveSecretKey(String repoKey) throws Exception {
        SecretKeyRepoData repoVal = null;
        if(REDIS_SPRING_CACHE_TYPE.equalsIgnoreCase(appEnv.getSpringCacheType())) {
            if(scalityRedisRepository.hasKey(repoKey)){
                repoVal = scalityRedisRepository.get(repoKey);
            }
        } else {
            repoVal = springLocalCache.get(repoKey);
        }

        String secretKey = null;

        if(repoVal != null) {

            // Using `repoKey` for Associated Data during encryption
            secretKey = cipherFactory.getCipherByID(repoVal.getKeyID())
                                    .decrypt(repoVal,
                                            cipherFactory.getSecretCipherKeyByID(repoVal.getKeyID()),
                                            repoKey);
        }
        return secretKey;
    }
}
