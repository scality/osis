/**
 * Copyright 2020 VMware, Inc.
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.security.config;

import com.scality.osis.security.basic.ScalityBasicAuthentication;
import com.scality.osis.security.platform.PlatformUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;


import static com.scality.osis.utils.ScalityConstants.HEALTH_CHECK_ENDPOINT;
import static com.scality.osis.security.jwt.AuthConstants.API_INFO;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

@Configuration
@EnableWebSecurity
@ConditionalOnProperty(value = "security.jwt.enabled",
        havingValue = "false",
        matchIfMissing = false)
@Order(HIGHEST_PRECEDENCE)
public class ScalityOsisBasicWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

    @Autowired
    private ScalityBasicAuthentication authentication;

    @Autowired
    private PlatformUserDetailsService service;

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(service).passwordEncoder(getPasswordEncoder());
        auth.eraseCredentials(true);
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.cors().and().csrf().disable();
        http.authorizeRequests().antMatchers(API_INFO).permitAll();
        http.authorizeRequests().antMatchers(HEALTH_CHECK_ENDPOINT).permitAll();
        http.authorizeRequests().anyRequest().authenticated();
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        http.httpBasic().authenticationEntryPoint(authentication);
    }

    @Bean
    public PasswordEncoder getPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
