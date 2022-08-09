/**
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.security.crypto;

import com.scality.osis.security.utils.YamlPropertySourceFactory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EnableConfigurationProperties(value = CryptoEnv.class)
@ConfigurationProperties(prefix="osis.security")
@PropertySources({
        /* Uncomment below line for only testing purposes */
//        @PropertySource(value="classpath:crypto.yml", factory = YamlPropertySourceFactory.class),
        @PropertySource(value="file:crypto.yml", factory = YamlPropertySourceFactory.class, ignoreResourceNotFound=true)
})
@Component
public class CryptoEnv {
    List<CipherKey> keys;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CipherKey {
        String id;
        String cipher;
        String secretKey;
    }
}
