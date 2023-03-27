/**
 * Copyright 2020 VMware, Inc.
 * Copyright 2022 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.security.jwt;

import com.scality.osis.security.config.OsisJwtWebSecurityConfigurerAdapter;
import com.scality.osis.security.jwt.extractor.JwtTokenExtractor;
import com.scality.osis.security.jwt.model.JwtAuthenticationToken;
import com.scality.osis.security.jwt.model.RawAccessToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This class extends the Spring AbstractAuthenticationProcessingFilter
 * and provides methods for processing JSON Web Tokens (JWTs) in the Scality OSIS API.
@see AbstractAuthenticationProcessingFilter
@see AuthenticationFailureHandler
@see JwtTokenExtractor
@see JwtAuthenticationToken
@see RawAccessToken
@see SecurityContext
 */
public class JwtTokenAuthenticationProcessingFilter extends AbstractAuthenticationProcessingFilter {
    private final AuthenticationFailureHandler failureHandler;
    private final JwtTokenExtractor tokenExtractor;

    /**
     * Constructor for JwtTokenAuthenticationProcessingFilter.
     *
     * @param failureHandler The AuthenticationFailureHandler used to handle authentication failures.
     * @param tokenExtractor The JwtTokenExtractor used to extract the token from the request.
     * @param matcher The RequestMatcher used to determine if the filter should be applied.
     */
    @Autowired
    public JwtTokenAuthenticationProcessingFilter(AuthenticationFailureHandler failureHandler,
            JwtTokenExtractor tokenExtractor, RequestMatcher matcher) {
        super(matcher);
        this.failureHandler = failureHandler;
        this.tokenExtractor = tokenExtractor;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {
        String tokenPayload = request.getHeader(OsisJwtWebSecurityConfigurerAdapter.AUTHENTICATION_HEADER_NAME);
        RawAccessToken token = new RawAccessToken(tokenExtractor.extract(tokenPayload));
        return getAuthenticationManager().authenticate(new JwtAuthenticationToken(token));
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
            Authentication authResult) throws IOException, ServletException {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authResult);
        SecurityContextHolder.setContext(context);
        chain.doFilter(request, response);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException failed) throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        failureHandler.onAuthenticationFailure(request, response, failed);
    }
}
