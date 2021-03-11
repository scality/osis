/**
 * Copyright 2020 VMware, Inc.
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scality.osis.platform.AppEnv;
import com.scality.osis.platform.security.ScalityUserDetailsService;
import com.scality.osis.security.jwt.JwtAuthenticationProvider;
import com.scality.osis.security.jwt.JwtTokenFactory;
import com.scality.osis.security.jwt.login.LoginAuthenticationFailureHandler;
import com.scality.osis.security.jwt.login.LoginAuthenticationProvider;
import com.scality.osis.security.jwt.login.LoginAuthenticationSuccessHandler;
import com.scality.osis.security.jwt.extractor.JwtHeaderTokenExtractor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
@ConditionalOnProperty(value = "security.jwt.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class OsisJwtBeans {

    @Bean
    protected BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    protected LoginAuthenticationProvider loginAuthenticationProvider(ScalityUserDetailsService userService, BCryptPasswordEncoder encoder) {
        return new LoginAuthenticationProvider(userService, encoder);
    }

    @Bean
    protected JwtAuthenticationProvider jwtAuthenticationProvider(AppEnv appEnv) {
        return new JwtAuthenticationProvider(appEnv);
    }

    @Bean
    protected JwtHeaderTokenExtractor jwtHeaderTokenExtractor() {
        return new JwtHeaderTokenExtractor();
    }

    @Bean
    protected JwtTokenFactory jwtTokenFactory(AppEnv appEnv) {
        return new JwtTokenFactory(appEnv);
    }

    @Bean
    protected LoginAuthenticationFailureHandler loginAuthenticationFailureHandler(ObjectMapper objectMapper) {
        return new LoginAuthenticationFailureHandler(objectMapper);
    }

    @Bean
    protected LoginAuthenticationSuccessHandler loginAuthenticationSuccessHandler(ObjectMapper objectMapper, JwtTokenFactory jwtTokenFactory) {
        return new LoginAuthenticationSuccessHandler(objectMapper, jwtTokenFactory);
    }
}
