/**
 *Copyright 2020 VMware, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.vmware.osis.scality.configuration;


import com.scality.vaultadmin.VaultAdmin;
import com.scality.vaultadmin.VaultAdminBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Configuration
public class ScalityRestConfig {

    @Value("${osis.scality.vault.access-key}")
    private String vaultAccessKey;

    @Value("${osis.scality.vault.secret-key}")
    private String vaultSecretKey;

    @Value("${osis.scality.vault.endpoint}")
    private String vaultEndpoint;

    @Bean
    public ClientHttpRequestFactory simpleClientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setReadTimeout(10000);
        factory.setConnectTimeout(10000);
        return factory;
    }

    @Bean
    public VaultAdmin getVaultAdmin() {
        return new VaultAdminBuilder()
                .accessKey(vaultAccessKey)
                .secretKey(vaultSecretKey)
                .endpoint(vaultEndpoint)
                .build();
    }
}
