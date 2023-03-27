/**
 *Copyright 2020 VMware, Inc.
 *Copyright 2022 Scality, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.security.jwt.login;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scality.osis.security.jwt.JwtTokenFactory;
import com.scality.osis.security.jwt.model.JwtResponse;
import com.scality.osis.security.jwt.model.JwtToken;
import com.scality.osis.security.jwt.model.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.scality.osis.security.jwt.AuthConstants.KEY_ACCESS_TOKEN;
import static com.scality.osis.security.jwt.AuthConstants.KEY_REFRESH_TOKEN;

/**
 * This class is an implementation of the Spring AuthenticationSuccessHandler interface.
 */
public class LoginAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectMapper mapper;
    private final JwtTokenFactory tokenFactory;

    /**
     * Constructor for LoginAuthenticationSuccessHandler.
     *
     * @param mapper The ObjectMapper used to serialize the error response.
     * @param tokenFactory The JwtTokenFactory used to generate the access and refresh tokens.
     * @see ObjectMapper
     * @see JwtTokenFactory
     */
    @Autowired
    public LoginAuthenticationSuccessHandler(final ObjectMapper mapper, final JwtTokenFactory tokenFactory) {
        this.mapper = mapper;
        this.tokenFactory = tokenFactory;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        UserContext userContext = (UserContext) authentication.getPrincipal();

        JwtToken accessToken = tokenFactory.createAccessJwtToken(userContext);
        JwtToken refreshToken = tokenFactory.createRefreshToken(userContext);

        Map<String, String> tokenMap = new HashMap<>();
        tokenMap.put(KEY_ACCESS_TOKEN, accessToken.getToken());
        tokenMap.put(KEY_REFRESH_TOKEN, refreshToken.getToken());
        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getWriter(), new JwtResponse(accessToken.getToken(), refreshToken.getToken()));
        clearAuthenticationAttributes(request);
    }

    /**
     * Removes temporary authentication-related data which may have been stored in the
     * session during the authentication process.
     *
     * @param request The HTTP request.
     * @see WebAttributes
     */
    protected final void clearAuthenticationAttributes(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
    }

}
