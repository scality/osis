/**
 * Copyright 2020 VMware, Inc.
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.security.config;

import com.scality.osis.security.basic.ScalityBasicAuthentication;
import com.scality.osis.security.platform.PlatformUserDetailsService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static com.scality.osis.security.jwt.AuthConstants.*;
import static com.scality.osis.utils.ScalityConstants.HEALTH_CHECK_ENDPOINT;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

@EnableWebSecurity
@Configuration
@ConditionalOnProperty(value = "security.jwt.enabled",
        havingValue = "false",
        matchIfMissing = false)
@Order(HIGHEST_PRECEDENCE)
public class ScalityOsisBasicWebSecurityConfigurerAdapter {

    private ScalityBasicAuthentication authentication;
    private PlatformUserDetailsService service;

    public ScalityOsisBasicWebSecurityConfigurerAdapter(ScalityBasicAuthentication authentication, PlatformUserDetailsService service) {
        this.authentication = authentication;
        this.service = service;
    }

    @Bean
    public AuthenticationManager authManager(HttpSecurity http, BCryptPasswordEncoder bCryptPasswordEncoder, PlatformUserDetailsService userDetailService) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .userDetailsService(userDetailService)
                .passwordEncoder(bCryptPasswordEncoder)
                .and()
                .eraseCredentials(true)
                .build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        return http.cors()
                .and()
                .csrf().disable()
                .authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests
                        .requestMatchers(API_INFO, HEALTH_CHECK_ENDPOINT, OPEN_API, SWAGGER).permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .httpBasic().authenticationEntryPoint(authentication)
                .and()
                .build();
    }

    @Bean
    public PasswordEncoder getPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
