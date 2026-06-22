/**
 * Copyright 2020 VMware, Inc.
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.service.tenant;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.AmazonIdentityManagementException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.amazonaws.util.StringUtils;
import com.scality.osis.ScalityAppEnv;
import com.scality.osis.s3.S3;
import com.scality.osis.service.impl.AsyncScalityOsisService;
import com.scality.osis.utapiclient.utils.UtapiClientException;
import com.scality.osis.utils.ScalityModelConverter;
import com.scality.osis.vaultadmin.VaultAdmin;
import com.scality.osis.vaultadmin.impl.VaultServiceException;
import com.scality.vaultclient.dto.AccountData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import static com.scality.osis.utils.ScalityConstants.ACCESS_DENIED;

/**
 * Default {@link TenantSession} backed by Scality Vault.
 *
 * <p>Owns the AssumeRole flow end to end: assume-role (with the access-denied to
 * {@code setupAssumeRole} recovery), region selection, IAM/S3 client construction,
 * and the single admin-policy retry.
 */
@Component
public class ScalityTenantSession implements TenantSession {

    private static final Logger logger = LoggerFactory.getLogger(ScalityTenantSession.class);

    private final ScalityAppEnv appEnv;
    private final VaultAdmin vaultAdmin;
    private final S3 s3;
    private final AsyncScalityOsisService asyncScalityOsisService;

    /**
     * Instantiates a new Scality tenant session.
     *
     * @param appEnv                  the app env
     * @param vaultAdmin              the vault admin
     * @param s3                      the s3 client
     * @param asyncScalityOsisService the async service that provisions the assume-role policies
     */
    public ScalityTenantSession(ScalityAppEnv appEnv, VaultAdmin vaultAdmin, S3 s3,
                                AsyncScalityOsisService asyncScalityOsisService) {
        this.appEnv = appEnv;
        this.vaultAdmin = vaultAdmin;
        this.s3 = s3;
        this.asyncScalityOsisService = asyncScalityOsisService;
    }

    @Override
    public AmazonIdentityManagement getIamClient(String tenantId) {
        return vaultAdmin.getIAMClient(getCredentials(tenantId), appEnv.getRegionInfo().get(0));
    }

    @Override
    public AmazonS3 getS3Client(String tenantId) {
        return s3.getS3Client(getCredentials(tenantId), appEnv.getRegionInfo().get(0));
    }

    @Override
    public <T> T run(String tenantId, TenantOp<T> op) throws Exception {
        try {
            return op.run();
        } catch (Exception e) {
            if (isAdminPolicyError(e) && !StringUtils.isNullOrEmpty(tenantId)) {
                generateAdminPolicy(tenantId);
                return op.run();
            }
            throw e;
        }
    }

    /**
     * Gets the tenant's temporary assume-role credentials.
     *
     * <p>On an access-denied response the role is recreated via
     * {@code setupAssumeRole} and the assume-role is retried.
     *
     * @param accountID the account id
     * @return the credentials
     */
    private Credentials getCredentials(String accountID) {
        Credentials credentials;
        try {
            AssumeRoleRequest assumeRoleRequest = ScalityModelConverter.getAssumeRoleRequestForAccount(accountID,
                    appEnv.getAssumeRoleName());
            logger.debug("[Vault] Assume Role request:{}", assumeRoleRequest);
            credentials = vaultAdmin.getTempAccountCredentials(assumeRoleRequest);
            logger.debug("[Vault] Assume Role response received with access key:{}, expiration:{}",
                    credentials.getAccessKeyId(), credentials.getExpiration());
        } catch (VaultServiceException e) {

            if (!StringUtils.isNullOrEmpty(e.getErrorCode()) &&
                    ACCESS_DENIED.equals(e.getErrorCode())) {
                // if access denied, the osis role is not provisioned yet: set it up and retry
                logger.debug("Assume role not ready for account {} ({}); recreating the role", accountID, e.getReason());
                // Call get Account with Account ID to retrieve account name
                AccountData account = vaultAdmin.getAccount(ScalityModelConverter.toGetAccountRequestWithID(accountID));
                asyncScalityOsisService.setupAssumeRole(accountID, account.getName());
                return getCredentials(accountID);
            }
            throw e;
        }
        return credentials;
    }

    private void generateAdminPolicy(String tenantId) throws Exception {
        AccountData account = vaultAdmin.getAccount(ScalityModelConverter.toGetAccountRequestWithID(tenantId));
        asyncScalityOsisService.setupAdminPolicy(tenantId, account.getName(), appEnv.getAssumeRoleName());
    }

    private boolean isAdminPolicyError(Exception e) {
        return (e instanceof AmazonIdentityManagementException &&
                (HttpStatus.FORBIDDEN.value() == ((AmazonIdentityManagementException) e).getStatusCode()))
                ||
                (e instanceof AmazonS3Exception &&
                        (HttpStatus.FORBIDDEN.value() == ((AmazonS3Exception) e).getStatusCode()))
                ||
                (e instanceof UtapiClientException &&
                        (HttpStatus.FORBIDDEN.value() == ((UtapiClientException) e).getStatusCode()));
    }
}
