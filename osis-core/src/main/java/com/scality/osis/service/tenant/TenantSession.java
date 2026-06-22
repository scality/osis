/**
 * Copyright 2020 VMware, Inc.
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.service.tenant;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.s3.AmazonS3;

/**
 * One home for the AssumeRole flow.
 *
 * <p>Every tenant-scoped IAM/S3 call needs the same prelude: assume the tenant's
 * {@code osis} role (recreating it via {@code setupAssumeRole} on an access-denied
 * response), pick the region, and build a client with the temporary credentials.
 * It also needs the same recovery: on a forbidden ("admin policy missing") error,
 * generate {@code adminPolicy@[account-id]} and retry once. This module owns all of
 * that so callers don't repeat it.
 */
public interface TenantSession {

    /**
     * Builds an IAM client backed by the tenant's assumed-role credentials.
     *
     * @param tenantId the storage account id
     * @return an IAM client scoped to the tenant
     */
    AmazonIdentityManagement getIamClient(String tenantId);

    /**
     * Builds an S3 client backed by the tenant's assumed-role credentials.
     *
     * @param tenantId the storage account id
     * @return an S3 client scoped to the tenant
     */
    AmazonS3 getS3Client(String tenantId);

    /**
     * Runs a tenant-scoped operation with admin-policy recovery.
     *
     * <p>Runs {@code op}; if it fails with a forbidden ("admin policy missing")
     * error, generates {@code adminPolicy@[account-id]} for the tenant and retries
     * {@code op} once. Any other failure, or a second failure, is rethrown.
     *
     * @param tenantId the storage account id
     * @param op       the operation to run
     * @param <T>      the result type
     * @return the result of {@code op}
     * @throws Exception the error raised by {@code op} (or by the single retry)
     */
    <T> T run(String tenantId, TenantOp<T> op) throws Exception;
}
