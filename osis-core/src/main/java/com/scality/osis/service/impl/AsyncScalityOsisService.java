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

/**
 * Async service to create a role with sts:AssumeRole permissions for the tenant
 * and generate an access key for the tenant
 */
@Component
public class AsyncScalityOsisService {
        private static final Logger logger = LoggerFactory.getLogger(AsyncScalityOsisService.class);

        @Autowired
        private ScalityAppEnv appEnv;

        @Autowired
        private VaultAdmin vaultAdmin;

        /**
         * Create a role with sts:AssumeRole permissions for the tenant
         *
         * @param osisTenant OsisTenant instance
         */
        @Async
        public void setupAssumeRole(OsisTenant osisTenant) {
                setupAssumeRole(osisTenant.getTenantId(), osisTenant.getName());
        }

        /**
         * Create a role with sts:AssumeRole permissions for the tenant
         *
         * @param tenantId tenant ID
         * @param tenantName tenant name
         */
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

        /**
         * Create and Attach Admin policy to the OSIS admin role
         *
         * @param tenantId Tenant Id
         * @param tenantName Tenant Name
         * @param assumeRoleName Assume Role Name
         * @throws Exception
         */
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
                                logger.debug("[Vault] Admin Policy already exists, detach role policy, delete it and create a new one");

                                DetachRolePolicyRequest detachRolePolicyRequest = ScalityModelConverter.toDetachAdminPolicyRequest(
                                        ScalityModelConverter.toAdminPolicyArn(tenantId),
                                        assumeRoleName);
                                logger.debug("[Vault] Detach Admin Policy Request:{}", new Gson().toJson(detachRolePolicyRequest));

                                DetachRolePolicyResult detachRolePolicyResult = iamClient.detachRolePolicy(detachRolePolicyRequest);
                                logger.debug("[Vault] Detach Admin Policy response:{}", new Gson().toJson(detachRolePolicyResult));

                                DeletePolicyRequest deletePolicyRequest = ScalityModelConverter.toDeleteAdminPolicyRequest(tenantId);
                                logger.debug("[Vault] Delete Admin Policy Request:{}", new Gson().toJson(detachRolePolicyRequest));

                                DeletePolicyResult deletePolicyResult = iamClient.deletePolicy(deletePolicyRequest);
                                logger.debug("[Vault] Delete Admin Policy Response:{}", new Gson().toJson(deletePolicyResult));

                                adminPolicyResult = iamClient.createPolicy(createAdminPolicyRequest);
                                logger.debug("[Vault] Create Admin Policy response:{}", new Gson().toJson(adminPolicyResult));

                        } else {
                                logger.error("Cannot create admin policy for Tenant ID:{}. " +
                                        "Exception details:{}", tenantId, e.getMessage());
                        }

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
