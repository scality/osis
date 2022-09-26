/**
 *Copyright 2020 VMware, Inc.
 *Copyright 2021 Scality, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis;

import com.scality.osis.vaultadmin.utils.VaultAdminUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.scality.osis.utils.ScalityConstants.*;

@Component
@Primary
public class ScalityAppEnv {
    public static final String COMMA = ",";

    @Autowired
    private Environment env;

    private String vaultSecretKey;

    public String getPlatformEndpoint() {
        return  env.getProperty("osis.scality.vault.endpoint");
    }

    public String getPlatformAccessKey() {
        return env.getProperty("osis.scality.vault.access-key");
    }

    public String getPlatformSecretKey() {
        if(StringUtils.isEmpty(vaultSecretKey) && isDecryptAdminCredentials()) {
            vaultSecretKey = VaultAdminUtils.getVaultSKEncryptedAdminFile(getPlatformAccessKey(),
                                                                        getAdminFilePath(),
                                                                        getMasterKeyFilePath());
        }

        if(StringUtils.isEmpty(vaultSecretKey)){
            vaultSecretKey = env.getProperty("osis.scality.vault.secret-key");
        }

        return vaultSecretKey;
    }

    public String getS3InterfaceEndpoint() {
        return env.getProperty("osis.scality.vaultS3Interface.endpoint");
    }

    public String getConsoleEndpoint() {
        return env.getProperty("osis.scality.console.endpoint");
    }

    public List<String> getStorageInfo() {
        String storageInfo = env.getProperty("osis.scality.storage-classes");
        if(StringUtils.isBlank(storageInfo)) {
            return Collections.singletonList("standard");
        }
        return Arrays.stream(StringUtils.split(storageInfo, COMMA)).
                map(String::trim).collect(Collectors.toList());
    }

    public List<String> getRegionInfo() {
        String regionInfo =  env.getProperty("osis.scality.region");
        if(StringUtils.isBlank(regionInfo)) {
            return Collections.singletonList("us-east-1");
        }
        return Arrays.stream(StringUtils.split(regionInfo, COMMA))
                .map(String::trim).collect(Collectors.toList());
    }

    public String getPlatformName() {
        return env.getProperty("osis.scality.name");
    }

    public String getPlatformVersion() {
        return env.getProperty("osis.scality.version");
    }

    public String getApiVersion() {
        return env.getProperty("osis.api.version");
    }

    public boolean isApiTokenEnabled() {
        return Boolean.parseBoolean(env.getProperty("security.jwt.enabled"));
    }

    public String getTokenIssuer() {
        return env.getProperty("security.jwt.token-issuer");
    }

    public int getAccessTokenExpirationTime() {
        return Integer.parseInt(env.getProperty("security.jwt.access-token-expiration-time"));
    }

    public String getTokenSigningKey() {
        return env.getProperty("security.jwt.token-signing-key");
    }

    public int getRefreshTokenExpirationTime() {
        return Integer.parseInt(env.getProperty("security.jwt.refresh_token_expiration_time"));
    }

    public String getAssumeRoleName() {
        return env.getProperty("osis.scality.vault.role.name");
    }

    public long getAccountAKDurationSeconds() {
        String durationSeconds =  env.getProperty("osis.scality.vault.account.accessKey.durationSeconds");
        if(StringUtils.isBlank(durationSeconds)) {
            durationSeconds = DEFAULT_ACCOUNT_AK_DURATION_SECONDS;
        }
        return Long.parseLong(durationSeconds);
    }

    public int getAsyncExecutorCorePoolSize() {
        String asyncExecutorCorePoolSize =  env.getProperty("osis.scality.async.corePoolSize");
        if(StringUtils.isBlank(asyncExecutorCorePoolSize)) {
            asyncExecutorCorePoolSize = DEFAULT_ASYNC_EXECUTOR_CORE_POOL_SIZE;
        }
        return Integer.parseInt(asyncExecutorCorePoolSize);
    }

    public int getAsyncExecutorMaxPoolSize() {
        String asyncExecutorMaxPoolSize =  env.getProperty("osis.scality.async.maxPoolSize");
        if(StringUtils.isBlank(asyncExecutorMaxPoolSize)) {
            asyncExecutorMaxPoolSize = DEFAULT_ASYNC_EXECUTOR_MAX_POOL_SIZE;
        }
        return Integer.parseInt(asyncExecutorMaxPoolSize);
    }

    public int getAsyncExecutorQueueCapacity() {
        String asyncExecutorQueueCapacity =  env.getProperty("osis.scality.async.queueCapacity");
        if(StringUtils.isBlank(asyncExecutorQueueCapacity)) {
            asyncExecutorQueueCapacity = DEFAULT_ASYNC_EXECUTOR_QUEUE_CAPACITY;
        }
        return Integer.parseInt(asyncExecutorQueueCapacity);
    }

    public String getSpringCacheType() {
        String cacheType = env.getProperty("spring.cache.type");
        if(StringUtils.isBlank(cacheType)) {
            cacheType = DEFAULT_SPRING_CACHE_TYPE;
        }
        return cacheType;
    }

    public boolean isDecryptAdminCredentials() {
        return Boolean.parseBoolean(env.getProperty("osis.scality.vault.decrypt-admin-credentials"));
    }

    public String getAdminFilePath() {
        return env.getProperty("osis.scality.vault.admin-file-path");
    }

    public String getMasterKeyFilePath() {
        return env.getProperty("osis.scality.vault.master-keyfile-path");
    }

    public int getVaultHealthCheckTimeout() {
        String vaultHealthCheckTimeout =  env.getProperty("osis.scality.vault.healthcheck.timeout");
        if(StringUtils.isBlank(vaultHealthCheckTimeout)) {
            vaultHealthCheckTimeout = DEFAULT_VAULT_HEALTHCHECK_TIMEOUT;
        }
        return Integer.parseInt(vaultHealthCheckTimeout);
    }

    public String getS3CapabilitiesFilePath() {
        return env.getProperty("osis.scality.s3.capabilities-file-path");
    }

    // function migrated from VMware Ceph implementation AppEnv
    public String getS3Endpoint() {
        return env.getProperty("osis.scality.s3.endpoint");
    }

    public int getS3HealthCheckTimeout() {
        String s3HealthCheckTimeout =  env.getProperty("osis.scality.s3.healthcheck.timeout");
        if(StringUtils.isBlank(s3HealthCheckTimeout)) {
            s3HealthCheckTimeout = DEFAULT_S3_HEALTHCHECK_TIMEOUT;
        }
        return Integer.parseInt(s3HealthCheckTimeout);
    }
}
