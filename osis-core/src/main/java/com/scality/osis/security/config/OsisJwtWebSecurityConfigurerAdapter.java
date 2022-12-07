/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scality.osis.security.jwt.JwtAuthenticationProvider;
import com.scality.osis.security.jwt.JwtTokenAuthenticationProcessingFilter;
import com.scality.osis.security.jwt.RestAuthenticationEntryPoint;
import com.scality.osis.security.jwt.SkipPathRequestMatcher;
import com.scality.osis.security.jwt.extractor.JwtTokenExtractor;
import com.scality.osis.security.jwt.login.LoginAuthenticationProvider;
import com.scality.osis.security.jwt.login.LoginProcessingFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

import static com.scality.osis.security.jwt.AuthConstants.API_INFO;

@EnableWebSecurity
@ConditionalOnProperty(value = "security.jwt.enabled", havingValue = "true", matchIfMissing = true)
public class OsisJwtWebSecurityConfigurerAdapter {

    public static final String AUTHENTICATION_HEADER_NAME = "Authorization";
    private static final String AUTHENTICATION_URL = "/api/v1/auth/login";
    private static final String REFRESH_TOKEN_URL = "/api/v1/auth/token";
    private static final String API_ROOT_URL = "/api/v1/**";

    private RestAuthenticationEntryPoint authenticationEntryPoint;
    private AuthenticationSuccessHandler successHandler;
    private AuthenticationFailureHandler failureHandler;
    private LoginAuthenticationProvider loginAuthenticationProvider;
    private JwtAuthenticationProvider jwtAuthenticationProvider;
    private JwtTokenExtractor tokenExtractor;
    private ObjectMapper objectMapper;

    public OsisJwtWebSecurityConfigurerAdapter(RestAuthenticationEntryPoint authenticationEntryPoint, AuthenticationSuccessHandler successHandler,
                                               AuthenticationFailureHandler failureHandler, LoginAuthenticationProvider loginAuthenticationProvider,
                                               JwtAuthenticationProvider jwtAuthenticationProvider,
                                               JwtTokenExtractor tokenExtractor, ObjectMapper objectMapper) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
        this.loginAuthenticationProvider = loginAuthenticationProvider;
        this.jwtAuthenticationProvider = jwtAuthenticationProvider;
        this.tokenExtractor = tokenExtractor;
        this.objectMapper = objectMapper;
    }

    protected LoginProcessingFilter buildLoginProcessingFilter(String loginEntryPoint, AuthenticationManager authenticationManager) {
        LoginProcessingFilter filter = new LoginProcessingFilter(loginEntryPoint, successHandler, failureHandler, objectMapper);
        filter.setAuthenticationManager(authenticationManager);
        return filter;
    }

    protected JwtTokenAuthenticationProcessingFilter buildJwtTokenAuthenticationProcessingFilter(
            List<String> pathsToSkip, String pattern, AuthenticationManager authenticationManager) {
        SkipPathRequestMatcher matcher = new SkipPathRequestMatcher(pathsToSkip, pattern);
        JwtTokenAuthenticationProcessingFilter filter = new JwtTokenAuthenticationProcessingFilter(failureHandler, tokenExtractor, matcher);
        filter.setAuthenticationManager(authenticationManager);
        return filter;
    }

    @Bean
    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .authenticationProvider(loginAuthenticationProvider)
                .authenticationProvider(jwtAuthenticationProvider)
                .build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        List<String> permitAllEndpoints = List.of(AUTHENTICATION_URL, REFRESH_TOKEN_URL, API_INFO, "/v3/api-docs/**", "/swagger*/**");
        var authenticationManager = http.getSharedObject(AuthenticationManager.class);

        return http.csrf().disable()
                .exceptionHandling()
                .authenticationEntryPoint(this.authenticationEntryPoint)
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests(authorizeRequests ->
                        authorizeRequests
                                .antMatchers(permitAllEndpoints.toArray(String[]::new)).permitAll()
                                .antMatchers(API_ROOT_URL).authenticated() // Protected API End-points
                )
                .addFilterBefore(buildLoginProcessingFilter(AUTHENTICATION_URL, authenticationManager),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(buildJwtTokenAuthenticationProcessingFilter(permitAllEndpoints,
                        API_ROOT_URL, authenticationManager), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
