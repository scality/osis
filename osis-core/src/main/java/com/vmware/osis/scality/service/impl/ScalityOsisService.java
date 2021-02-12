/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.vmware.osis.scality.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.scality.vaultclient.dto.CreateAccountRequestDTO;
import com.scality.vaultclient.dto.CreateAccountResponseDTO;
import com.vmware.osis.model.exception.NotImplementedException;
import com.vmware.osis.scality.AppEnv;
import com.vmware.osis.scality.utils.ModelConverter;
import com.vmware.osis.model.*;
import com.vmware.osis.resource.OsisCapsManager;
import com.vmware.osis.scality.utils.ScalityUtil;
import com.vmware.osis.service.OsisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import com.scality.vaultadmin.VaultAdmin;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.vmware.osis.scality.utils.ScalityConstants.IAM_PREFIX;


@Service
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

    public ScalityOsisService(VaultAdmin vaultAdmin){
        this.vaultAdmin = vaultAdmin;
    }

    @Override
    public OsisTenant createTenant(OsisTenant osisTenant) {
        CreateAccountRequestDTO accountRequest = ModelConverter.toScalityAccountRequest(osisTenant);

        return ModelConverter.toOsisTenant(vaultAdmin.createAccount(accountRequest));
    }

    @Override
    public PageOfTenants queryTenants(long offset, long limit, String filter) {
        PageOfTenants pageOfTenants = mockListTenants(offset, limit);
        if(filter.contains("cd_tenant_id")){
            PageOfTenants page = new PageOfTenants();
            String cd = filter.substring(filter.indexOf("==")+2);

            List<OsisTenant> items = pageOfTenants.getItems().stream().filter(tenant -> tenant.getCdTenantIds().contains(cd)).collect(Collectors.toList());
            page.setItems(items);
            page.setPageInfo(pageOfTenants.getPageInfo());
            return page;
        } else{
            return pageOfTenants;
        }

    }

    @Override
    public PageOfTenants listTenants(long offset, long limit) {
        return mockListTenants(offset, limit);
    }

    private PageOfTenants mockListTenants(long offset, long limit) {
        PageOfTenants pageOfTenants = new PageOfTenants();

        //tenant-1
        String tenant1Res = "{\"account\":{\"data\":{\"id\":\"2c3e3e2908fb455893f09766977da58e\",\"emailAddress\":\"2c3e3e29-08fb-4558-93f0-9766977da58e@osis.account.com\",\"name\":\"sample-3__2c3e3e2908fb455893f09766977da58e\",\"arn\":\"arn:aws:iam::799467728277:/sai/\",\"createDate\":\"2021-02-12T21:17:21Z\"}}}";
        OsisTenant tenant1 = ModelConverter.toOsisTenant(new Gson().fromJson(tenant1Res, CreateAccountResponseDTO.class));

        //tenant-2
        String tenant2Res = "{\"account\":{\"data\":{\"id\":\"eb5d6308a7e64d6a813964534366a7ae\",\"emailAddress\":\"eb5d6308-a7e6-4d6a-8139-64534366a7ae@osis.account.com\",\"name\":\"sample-org__eb5d6308a7e64d6a813964534366a7ae\",\"arn\":\"arn:aws:iam::eb5d6308a7e64d6a813964534366a7ae:/sample-org__eb5d6308a7e64d6a813964534366a7ae/\",\"createDate\":\"2021-02-12T21:17:21Z\"}}}";
        OsisTenant tenant2 = ModelConverter.toOsisTenant(new Gson().fromJson(tenant2Res, CreateAccountResponseDTO.class));

        //tenant-3
        String tenant3Res = "{\"account\":{\"data\":{\"id\":\"8b45caac331d49deb1fc73e8cc77ddaa\",\"emailAddress\":\"8b45caac-331d-49de-b1fc-73e8cc77ddaa@osis.account.com\",\"name\":\"sample-tenant2__8b45caac331d49deb1fc73e8cc77ddaa\",\"arn\":\"arn:aws:iam::8b45caac331d49deb1fc73e8cc77ddaa:/sample-tenant2__8b45caac331d49deb1fc73e8cc77ddaa/\",\"createDate\":\"2021-02-12T21:17:21Z\"}}}";
        OsisTenant tenant3 = ModelConverter.toOsisTenant(new Gson().fromJson(tenant3Res, CreateAccountResponseDTO.class));

        List<OsisTenant> items = new ArrayList<>();
        items.add(tenant1);
        items.add(tenant2);
        items.add(tenant3);

        PageInfo pageInfo = new PageInfo();
        pageInfo.setLimit(limit);
        pageInfo.setOffset(offset);
        pageInfo.setTotal(3l);

        pageOfTenants.items(items);
        pageOfTenants.setPageInfo(pageInfo);
        return pageOfTenants;
    }

    @Override
    public OsisUser createUser(OsisUser osisUser) {
        osisUser.setUserId(ScalityUtil.normalize(osisUser.getCdUserId()));
        logger.info("create user request:"+ new Gson().toJson(osisUser));
        return osisUser;
    }

    @Override
    public PageOfUsers queryUsers(long offset, long limit, String filter) {
        return mockListUsers(offset, limit);
    }

    private PageOfUsers mockListUsers(long offset, long limit) {

        String user1 ="{\"userId\":\"5ef7c754ba3540589c3ac36cce2796ad\",\"tenantId\":\"eb5d6308a7e64d6a813964534366a7ae\",\"active\":true,\"cdUserId\":\"5ef7c754-ba35-4058-9c3a-c36cce2796ad\",\"cdTenantId\":\"eb5d6308-a7e6-4d6a-8139-64534366a7ae\",\"username\":\"test\",\"role\":\"TENANT_USER\"}";
        //user1
        OsisUser osisUser1 = new Gson().fromJson(user1,OsisUser.class);


        String user2 ="{\"userId\":\"94bf30e3f590491196d541e3dc309c69\",\"tenantId\":\"eb5d6308a7e64d6a813964534366a7ae\",\"active\":true,\"cdUserId\":\"94bf30e3-f590-4911-96d5-41e3dc309c69\",\"cdTenantId\":\"eb5d6308-a7e6-4d6a-8139-64534366a7ae\",\"username\":\"d4it2awsb588go5j9t00\",\"role\":\"TENANT_USER\"}";
        //user2
        OsisUser osisUser2 = new Gson().fromJson(user2,OsisUser.class);

        List<OsisUser> users = new ArrayList<>();
        users.add(osisUser1);
        users.add(osisUser2);


        PageInfo pageInfo = new PageInfo();
        pageInfo.setLimit(limit);
        pageInfo.setOffset(offset);
        pageInfo.setTotal(2l);

        PageOfUsers pageOfUsers = new PageOfUsers();
        pageOfUsers.setItems(users);
        pageOfUsers.setPageInfo(pageInfo);
        return pageOfUsers;
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
        return tenantId.equals("8b45caac331d49deb1fc73e8cc77ddaa") ||
                tenantId.equals("eb5d6308a7e64d6a813964534366a7ae") ||
                tenantId.equals("2c3e3e2908fb455893f09766977da58e");
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
        return mockListUsers(offset,limit);
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
                .logoUri(ScalityUtil.getLogoUri(domain))
                .services(new InformationServices().iam(domain + IAM_PREFIX).s3(appEnv.getS3Endpoint()))
                .status(ScalityUtil.checkScalityStatus(vaultAdmin));
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
