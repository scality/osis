/**
 * Copyright 2020 VMware, Inc.
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.security.jwt.endpoint;

import com.scality.osis.platform.AppEnv;
import com.scality.osis.platform.security.ScalityUserDetailsService;
import com.scality.osis.security.jwt.AuthConstants;
import com.scality.osis.security.jwt.JwtTokenFactory;
import com.scality.osis.security.jwt.model.RawAccessToken;
import com.scality.osis.security.jwt.model.RefreshToken;
import com.scality.osis.security.jwt.model.RefreshTokenRequest;
import com.scality.osis.security.jwt.model.RefreshTokenResponse;
import com.scality.osis.security.jwt.model.UserContext;
import com.scality.osis.security.jwt.model.exception.InvalidJwtTokenException;
import com.scality.osis.security.jwt.verifier.JwtTokenVerifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;

@ConditionalOnProperty(value = "security.jwt.enabled",
        havingValue = "true",
        matchIfMissing = true)
@RestController
public class RefreshTokenEndpoint {
    @Autowired
    private JwtTokenFactory tokenFactory;
    @Autowired
    private ScalityUserDetailsService userService;
    @Autowired
    private JwtTokenVerifier tokenVerifier;
    @Autowired
    private AppEnv appEnv;

    @PostMapping(value = "/api/v1/auth/token", produces = {MediaType.APPLICATION_JSON_VALUE})
    public @ResponseBody
    RefreshTokenResponse refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest, HttpServletResponse response) {
        RawAccessToken rawToken = new RawAccessToken(refreshTokenRequest.getRefreshToken());
        RefreshToken refreshToken = RefreshToken.create(rawToken, appEnv.getTokenSigningKey()).orElseThrow(InvalidJwtTokenException::new);

        String jti = refreshToken.getJti();
        if (!tokenVerifier.verify(jti)) {
            throw new InvalidJwtTokenException();
        }

        String subject = refreshToken.getSubject();
        UserDetails user = userService.loadUserByUsername(subject);
        if (user == null) {
            throw new UsernameNotFoundException(String.format("User not found: %s", subject));
        }

        UserContext userContext = UserContext.create(user.getUsername(), Arrays.asList(new SimpleGrantedAuthority(AuthConstants.ROLE_ADMIN)));

        return new RefreshTokenResponse(tokenFactory.createAccessJwtToken(userContext).getToken());
    }
}
