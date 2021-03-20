/**
 *Copyright 2020 VMware, Inc.
 *Copyright 2021 Scality, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.configuration;


import com.scality.osis.vaultadmin.VaultAdmin;
import com.scality.osis.vaultadmin.VaultAdminBuilder;
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
                .setAccessKey(vaultAccessKey)
                .setSecretKey(vaultSecretKey)
                .setEndpoint(vaultEndpoint)
                .build();
    }
}
