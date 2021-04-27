/**
 *Copyright 2020 VMware, Inc.
 *Copyright 2021 Scality, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis;

import com.vmware.osis.platform.AppEnv;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.scality.osis.utils.ScalityConstants.DEFAULT_ACCOUNT_AK_DURATION_SECONDS;

@Component
@Primary
public class ScalityAppEnv extends AppEnv {
    public static final String COMMA = ",";

    @Autowired
    private Environment env;

    public String getPlatformEndpoint() {
        return  env.getProperty("osis.scality.vault.endpoint");
    }

    public String getPlatformAccessKey() {
        return env.getProperty("osis.scality.vault.access-key");
    }

    public String getPlatformSecretKey() {
        return env.getProperty("osis.scality.vault.secret-key");
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
            return Collections.singletonList("default");
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
}
