/**
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.service.impl;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.*;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.google.gson.Gson;
import com.scality.osis.ScalityAppEnv;
import com.scality.osis.model.OsisTenant;
import com.scality.osis.utils.ScalityModelConverter;
import com.scality.osis.vaultadmin.VaultAdmin;
import com.scality.vaultclient.dto.GenerateAccountAccessKeyRequest;
import com.scality.vaultclient.dto.GenerateAccountAccessKeyResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static com.scality.osis.utils.ScalityConstants.DEFAULT_ADMIN_POLICY_DOCUMENT;

@Component
public class AsyncScalityOsisService {
        private static final Logger logger = LoggerFactory.getLogger(AsyncScalityOsisService.class);

        @Autowired
        private ScalityAppEnv appEnv;

        @Autowired
        private VaultAdmin vaultAdmin;

        @Async
        public void setupAssumeRole(OsisTenant osisTenant) {
                setupAssumeRole(osisTenant.getTenantId(), osisTenant.getName());
        }

        public void setupAssumeRole(String tenantId, String tenantName) {
                try {
                        logger.info(" Started [setupAssumeRole] for the Tenant:{}", tenantId);

                        /*
                         * Call generateAccountAccessKey() and extract credentials from
                         * `generateAccountAccessKeyResponse`
                         */
                        GenerateAccountAccessKeyRequest generateAccountAccessKeyRequest = ScalityModelConverter
                                        .toGenerateAccountAccessKeyRequest(
                                                        tenantName,
                                                        appEnv.getAccountAKDurationSeconds());
                        logger.debug("[Vault] Generate Account AccessKey Request:{}",
                                        new Gson().toJson(generateAccountAccessKeyRequest));

                        GenerateAccountAccessKeyResponse generateAccountAccessKeyResponse = vaultAdmin
                                        .getAccountAccessKey(generateAccountAccessKeyRequest);

                        logger.info("Generated temporary Account AccessKey accessKey={} for account ID={} with expiration={}",
                                        generateAccountAccessKeyResponse.getData().getId(), tenantId,
                                        generateAccountAccessKeyResponse.getData().getNotAfter());
                        logger.trace("[Vault] Generate Account AccessKey full response:{}",
                                        new Gson().toJson(generateAccountAccessKeyResponse));

                        Credentials credentials = ScalityModelConverter.toCredentials(generateAccountAccessKeyResponse);

                        AmazonIdentityManagement iamClient = vaultAdmin.getIAMClient(credentials,
                                        appEnv.getRegionInfo().get(0));

                        /* Create OSIS admin role with sts:AssumeRole permissions */
                        CreateRoleRequest createOSISRoleRequest = ScalityModelConverter
                                        .toCreateOSISRoleRequest(appEnv.getAssumeRoleName());

                        logger.debug("[Vault] Create OSIS role Request:{}", new Gson().toJson(createOSISRoleRequest));

                        CreateRoleResult createOSISRoleResponse = iamClient.createRole(createOSISRoleRequest);

                        logger.debug("[Vault] Create OSIS role response:{}", new Gson().toJson(createOSISRoleResponse));

                        // Create and Attach Admin policy to the OSIS admin role
                        createAttachAdminPolicy(tenantId, createOSISRoleResponse.getRole().getRoleName(), iamClient);

                        // Delete the newly created Access Key for the account
                        DeleteAccessKeyRequest deleteAccessKeyRequest = ScalityModelConverter
                                        .toDeleteAccessKeyRequest(credentials.getAccessKeyId(), null);
                        logger.debug("[Vault] Delete Access key Request:{}", new Gson().toJson(deleteAccessKeyRequest));

                        DeleteAccessKeyResult deleteAccessKeyResult = iamClient.deleteAccessKey(deleteAccessKeyRequest);
                        logger.debug("[Vault] Delete Access key response:{}", new Gson().toJson(deleteAccessKeyResult));

                        logger.info(" Finished [setupAssumeRole] for the Tenant:{}", tenantId);

                } catch (Exception e) {
                        logger.error("setupAssumeRole error for Tenant id:{}. Error details: ", tenantId, e);
                }
        }

        public void setupAdminPolicy(String tenantId, String tenantName, String assumeRoleName) throws Exception {
                logger.info(" Started [setupAdminPolicy] for the Tenant:{}", tenantId);

                /*
                 * Call generateAccountAccessKey() and extract credentials from
                 * `generateAccountAccessKeyResponse`
                 */
                GenerateAccountAccessKeyRequest generateAccountAccessKeyRequest = ScalityModelConverter
                                .toGenerateAccountAccessKeyRequest(
                                                tenantName,
                                                appEnv.getAccountAKDurationSeconds());
                logger.debug("[Vault] Generate Account AccessKey Request:{}",
                                new Gson().toJson(generateAccountAccessKeyRequest));

                GenerateAccountAccessKeyResponse generateAccountAccessKeyResponse = vaultAdmin
                                .getAccountAccessKey(generateAccountAccessKeyRequest);

                logger.info("Generated temporary Account AccessKey accessKey={} for account ID={} with expiration={}",
                                generateAccountAccessKeyResponse.getData().getId(), tenantId,
                                generateAccountAccessKeyResponse.getData().getNotAfter());
                logger.trace("[Vault] Generate Account AccessKey full response:{}",
                                new Gson().toJson(generateAccountAccessKeyResponse));

                Credentials credentials = ScalityModelConverter.toCredentials(generateAccountAccessKeyResponse);

                AmazonIdentityManagement iamClient = vaultAdmin.getIAMClient(credentials,
                                appEnv.getRegionInfo().get(0));

                // Create and Attach Admin policy to the OSIS admin role
                createAttachAdminPolicy(tenantId, assumeRoleName, iamClient);

                // Delete the newly created Access Key for the account
                DeleteAccessKeyRequest deleteAccessKeyRequest = ScalityModelConverter
                                .toDeleteAccessKeyRequest(credentials.getAccessKeyId(), null);
                logger.debug("[Vault] Delete Access key Request:{}", new Gson().toJson(deleteAccessKeyRequest));

                DeleteAccessKeyResult deleteAccessKeyResult = iamClient.deleteAccessKey(deleteAccessKeyRequest);
                logger.debug("[Vault] Delete Access key response:{}", new Gson().toJson(deleteAccessKeyResult));

                logger.info(" Finished [setupAdminPolicy] for the Tenant:{}", tenantId);

        }

        private void createAttachAdminPolicy(String tenantId, String assumeRoleName,
                        AmazonIdentityManagement iamClient) {
                /* Create Admin policy for account `adminPolicy@[account-id]` */
                CreatePolicyRequest createAdminPolicyRequest = ScalityModelConverter
                                .toCreateAdminPolicyRequest(tenantId);
                logger.debug("[Vault] Create Admin Policy Request:{}", new Gson().toJson(createAdminPolicyRequest));

                CreatePolicyResult adminPolicyResult = null;
                try {
                        adminPolicyResult = iamClient.createPolicy(createAdminPolicyRequest);
                        logger.debug("[Vault] Create Admin Policy response:{}", new Gson().toJson(adminPolicyResult));

                } catch (Exception e) {
                        if (e instanceof AmazonIdentityManagementException &&
                                ((AmazonIdentityManagementException) e).getStatusCode() == HttpStatus.CONFLICT.value()){
                                logger.debug("[Vault] Admin Policy already exists, checking if policy document changes");

                                GetPolicyRequest getAdminPolicyRequest = ScalityModelConverter.toGetAdminPolicyRequest(tenantId);
                                logger.debug("[Vault] Get Admin Policy Request:{}", new Gson().toJson(getAdminPolicyRequest));

                                GetPolicyResult getAdminPolicyResult = iamClient.getPolicy(getAdminPolicyRequest);
                                String defaultPolicyVersionId = getAdminPolicyResult.getPolicy().getDefaultVersionId();
                                logger.debug("[Vault] Get Admin Policy Result:{}, defaultPolicyVersionId: {}",
                                        new Gson().toJson(getAdminPolicyResult), defaultPolicyVersionId);

                                GetPolicyVersionRequest getAdminPolicyVersionRequest = ScalityModelConverter.toGetAdminPolicyVersionRequest(tenantId, defaultPolicyVersionId);
                                logger.debug("[Vault] Get Admin Policy Default Version Request:{}", new Gson().toJson(getAdminPolicyVersionRequest));

                                GetPolicyVersionResult getAdminPolicyVersionResult =  iamClient.getPolicyVersion(getAdminPolicyVersionRequest);
                                logger.debug("[Vault] Get Admin Policy Default Version result:{}", new Gson().toJson(getAdminPolicyVersionResult));

                                if (!Objects.equals(URLDecoder.decode(getAdminPolicyVersionResult.getPolicyVersion().getDocument(), StandardCharsets.UTF_8),
                                        URLDecoder.decode(DEFAULT_ADMIN_POLICY_DOCUMENT, StandardCharsets.UTF_8))) {
                                        logger.debug("Default Admin Policy Document changed, before: {}, current: {}",
                                                getAdminPolicyVersionResult.getPolicyVersion().getDocument(),
                                                DEFAULT_ADMIN_POLICY_DOCUMENT);
                                        CreatePolicyVersionRequest createAdminPolicyVersionRequest = ScalityModelConverter.toCreateAdminPolicyVersionRequest(tenantId);
                                        logger.debug("[Vault] Create new Admin Policy Version Request:{}", new Gson().toJson(createAdminPolicyVersionRequest));
                                        CreatePolicyVersionResult createAdminPolicyVersionResult = iamClient.createPolicyVersion(createAdminPolicyVersionRequest);
                                        logger.debug("[Vault] Create new Admin Policy Version Result:{}", new Gson().toJson(createAdminPolicyVersionResult));
                                        return;
                                }
                        }
                        logger.error("Cannot create admin policy for Tenant ID:{}. " +
                                "Exception details:{}", tenantId, e.getMessage());
                }
                /* Attach admin policy to `osis` role */
                AttachRolePolicyRequest attachAdminPolicyRequest = ScalityModelConverter.toAttachAdminPolicyRequest(
                                (adminPolicyResult != null) ? adminPolicyResult.getPolicy().getArn()
                                                : ScalityModelConverter.toAdminPolicyArn(tenantId),
                                assumeRoleName);

                logger.debug("[Vault] Attach Admin Policy Request:{}", new Gson().toJson(attachAdminPolicyRequest));

                AttachRolePolicyResult attachAdminPolicyResult = iamClient.attachRolePolicy(attachAdminPolicyRequest);
                logger.debug("[Vault] Attach Admin Policy response:{}", new Gson().toJson(attachAdminPolicyResult));
        }
}
