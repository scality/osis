/**
 * Copyright 2020 VMware, Inc.
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.service.tenant;

/**
 * A unit of work executed against a tenant's assumed-role session.
 *
 * <p>Used by {@link TenantSession#run(String, TenantOp)} so the admin-policy
 * recovery (generate {@code adminPolicy@[account-id]} then retry once) can wrap
 * an arbitrary tenant-scoped operation.
 *
 * @param <T> the result type of the operation
 */
@FunctionalInterface
public interface TenantOp<T> {

    /**
     * Runs the tenant-scoped operation.
     *
     * @return the result of the operation
     * @throws Exception any error raised while running the operation
     */
    T run() throws Exception;
}
