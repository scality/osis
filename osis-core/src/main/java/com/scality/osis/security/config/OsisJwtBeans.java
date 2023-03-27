/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scality.osis.ScalityAppEnv;
import com.scality.osis.security.platform.PlatformUserDetailsService;
import com.scality.osis.security.jwt.JwtAuthenticationProvider;
import com.scality.osis.security.jwt.extractor.JwtHeaderTokenExtractor;
import com.scality.osis.security.jwt.login.LoginAuthenticationFailureHandler;
import com.scality.osis.security.jwt.login.LoginAuthenticationProvider;
import com.scality.osis.security.jwt.login.LoginAuthenticationSuccessHandler;
import com.scality.osis.security.jwt.JwtTokenFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * This class defines several Spring Beans used for authentication and token management
 * in the OSIS application. It provides methods for creating and configuring Spring
 * Beans that handle password encryption, user authentication, token creation, and
 * authentication success/failure.
 */
@Configuration
@ConditionalOnProperty(value = "security.jwt.enabled", havingValue = "true", matchIfMissing = true)
public class OsisJwtBeans {

    /**
     * Creates a new LoginAuthenticationSuccessHandler.
     *
     * @return the LoginAuthenticationSuccessHandler
     */
    @Bean
    protected BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Creates a new LoginAuthenticationProvider.
     *
     * @param userService the user service
     * @param encoder the password encoder
     * @return the LoginAuthenticationProvider
     */
    @Bean
    protected LoginAuthenticationProvider loginAuthenticationProvider(PlatformUserDetailsService userService,
            BCryptPasswordEncoder encoder) {
        return new LoginAuthenticationProvider(userService, encoder);
    }

    /**
     * creates a new instance of the JwtAuthenticationProvider class, which is responsible for
     * authenticating a user via a JWT token. It takes in an instance of ScalityAppEnv which
     * contains configuration details for the Spring Boot application.
     *
     * @param appEnv the application environment
     * @return the JwtAuthenticationProvider
     */
    @Bean
    protected JwtAuthenticationProvider jwtAuthenticationProvider(ScalityAppEnv appEnv) {
        return new JwtAuthenticationProvider(appEnv);
    }

    /**
     * Creates a new JwtHeaderTokenExtractor.
     *
     * @return the JwtHeaderTokenExtractor
     */
    @Bean
    protected JwtHeaderTokenExtractor jwtHeaderTokenExtractor() {
        return new JwtHeaderTokenExtractor();
    }

    /**
     * Creates a new JwtTokenFactory.
     *
     * @param appEnv the application environment
     * @return the JwtTokenFactory
     */
    @Bean
    protected JwtTokenFactory jwtTokenFactory(ScalityAppEnv appEnv) {
        return new JwtTokenFactory(appEnv);
    }

    /**
     * Creates a new LoginAuthenticationFailureHandler.
     *
     * @param objectMapper the object mapper
     * @return the LoginAuthenticationFailureHandler
     */
    @Bean
    protected LoginAuthenticationFailureHandler loginAuthenticationFailureHandler(ObjectMapper objectMapper) {
        return new LoginAuthenticationFailureHandler(objectMapper);
    }

    /**
     * Creates a new LoginAuthenticationSuccessHandler.
     *
     * @param objectMapper the object mapper
     * @param jwtTokenFactory the JWT token factory
     * @return the LoginAuthenticationSuccessHandler
     */
    @Bean
    protected LoginAuthenticationSuccessHandler loginAuthenticationSuccessHandler(ObjectMapper objectMapper,
            JwtTokenFactory jwtTokenFactory) {
        return new LoginAuthenticationSuccessHandler(objectMapper, jwtTokenFactory);
    }
}
