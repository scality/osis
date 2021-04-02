/**
 * Copyright 2020 VMware, Inc.
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.service.impl;

import com.scality.osis.utils.ScalityUtils;
import com.google.gson.Gson;
import com.scality.osis.utils.ScalityModelConverter;
import com.scality.osis.vaultadmin.VaultAdmin;
import com.scality.osis.vaultadmin.impl.VaultServiceException;
import com.scality.vaultclient.dto.CreateAccountRequestDTO;
import com.scality.vaultclient.dto.CreateAccountResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scality.vaultclient.dto.ListAccountsRequestDTO;
import com.scality.vaultclient.dto.ListAccountsResponseDTO;
import com.vmware.osis.platform.AppEnv;
import com.vmware.osis.model.*;
import com.vmware.osis.model.exception.NotImplementedException;
import com.vmware.osis.resource.OsisCapsManager;
import com.vmware.osis.service.OsisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

import static com.scality.osis.utils.ScalityConstants.CD_TENANT_ID_PREFIX;
import static com.scality.osis.utils.ScalityConstants.IAM_PREFIX;


@Service
@Primary
public class ScalityOsisService implements OsisService {
    private static final Logger logger = LoggerFactory.getLogger(ScalityOsisService.class);
    private static final String S3_CAPABILITIES_JSON = "s3capabilities.json";

    @Autowired
    private AppEnv appEnv;

    @Autowired
    private VaultAdmin vaultAdmin;

    @Autowired
    private OsisCapsManager osisCapsManager;

    public ScalityOsisService(){}

    public ScalityOsisService(AppEnv appEnv, VaultAdmin vaultAdmin, OsisCapsManager osisCapsManager){
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

            logger.info("Create Tenant response:{}", new Gson().toJson(resOsisTenant));

            return resOsisTenant;
        } catch (VaultServiceException e){
            // Create Tenant supports only 400:BAD_REQUEST error, change status code in the VaultServiceException
            throw new VaultServiceException(HttpStatus.BAD_REQUEST.value(), e.getMessage(), e);
        }
    }

    @Override
    public PageOfTenants queryTenants(long offset, long limit, String filter) {
        if(filter.contains(CD_TENANT_ID_PREFIX)) {
            try {
                logger.info("Query Tenants request received: offset={}, limit={}, filter ={}", offset, limit, filter);
                ListAccountsRequestDTO listAccountsRequest = ScalityModelConverter.toScalityListAccountsRequest(limit, filter);

                logger.debug("[Vault] List Accounts Request:{}", new Gson().toJson(listAccountsRequest));
                ListAccountsResponseDTO listAccountsResponseDTO = vaultAdmin.listAccounts(offset, listAccountsRequest);

                logger.debug("[Vault] List Accounts response:{}", new Gson().toJson(listAccountsResponseDTO));

                PageOfTenants pageOfTenants = ScalityModelConverter.toPageOfTenants(listAccountsResponseDTO, offset, limit);

                logger.info("Query Tenants response:{}", new Gson().toJson(pageOfTenants));

                return pageOfTenants;

            } catch (VaultServiceException e) {
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
        throw new NotImplementedException();
    }

    @Override
    public PageOfUsers queryUsers(long offset, long limit, String filter) {
        throw new NotImplementedException();
    }


    @Override
    public OsisS3Credential createS3Credential(String tenantId, String userId) {
        throw new NotImplementedException();
    }

    @Override
    public PageOfS3Credentials queryS3Credentials(long offset, long limit, String filter) {
        throw new NotImplementedException();
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
    public OsisTenant getTenant(String tenantId) {
        throw new NotImplementedException();
    }

    @Override
    public OsisUser getUser(String canonicalUserId) {
        throw new NotImplementedException();
    }

    @Override
    public OsisUser getUser(String tenantId, String userId) {
        throw new NotImplementedException();

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
        throw new NotImplementedException();
    }

    @Override
    public PageOfUsers listUsers(String tenantId, long offset, long limit) {
        throw new NotImplementedException();
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
                .services(new InformationServices().iam(domain + IAM_PREFIX).s3(appEnv.getS3Endpoint()))
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
}
