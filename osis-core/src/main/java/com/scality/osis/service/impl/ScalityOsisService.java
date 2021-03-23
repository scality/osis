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
import com.scality.vaultclient.dto.CreateAccountRequestDTO;
import com.scality.vaultclient.dto.CreateAccountResponseDTO;
import com.vmware.osis.platform.AppEnv;
import com.vmware.osis.model.*;
import com.vmware.osis.model.exception.NotImplementedException;
import com.vmware.osis.resource.OsisCapsManager;
import com.vmware.osis.service.OsisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Optional;

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
        logger.info("Create Tenant request received:{}", new Gson().toJson(osisTenant));
        CreateAccountRequestDTO accountRequest = ScalityModelConverter.toScalityAccountRequest(osisTenant);

        logger.debug("[Vault]CreateAccount request:{}", new Gson().toJson(accountRequest));

        CreateAccountResponseDTO accountResponse = vaultAdmin.createAccount(accountRequest);

        logger.debug("[Vault]CreateAccount response:{}", new Gson().toJson(accountResponse));

        OsisTenant resOsisTenant = ScalityModelConverter.toOsisTenant(accountResponse);

        logger.info("Create Tenant response:{}", new Gson().toJson(resOsisTenant));

        return resOsisTenant;
    }

    @Override
    public PageOfTenants queryTenants(long offset, long limit, String filter) {
        throw new NotImplementedException();
    }

    @Override
    public PageOfTenants listTenants(long offset, long limit) {
        throw new NotImplementedException();
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
        throw new NotImplementedException();
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
        throw new NotImplementedException();
    }
}
