/**
 *Copyright 2020 VMware, Inc.
 *Copyright 2022 Scality, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.security.jwt;

import com.scality.osis.ScalityAppEnv;
import com.scality.osis.security.jwt.model.AccessToken;
import com.scality.osis.security.jwt.model.JwtToken;
import com.scality.osis.security.jwt.model.Scopes;
import com.scality.osis.security.jwt.model.UserContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.scality.osis.security.jwt.AuthConstants.CLAIMS_SCOPES;

/**
 * Factory class for creating JWT tokens.
 */
public class JwtTokenFactory {
    private final ScalityAppEnv appEnv;

    /**
     * Constructs a new JwtTokenFactory.
     *
     * @param appEnv the application environment
     */
    public JwtTokenFactory(ScalityAppEnv appEnv) {
        this.appEnv = appEnv;
    }

    /**
     * Creates a new access token.
     *
     * @param userContext the user context
     * @return the access token
     */
    public AccessToken createAccessJwtToken(UserContext userContext) {
        if (StringUtils.isBlank(userContext.getUsername()))
            throw new IllegalArgumentException("Username is required to create token.");

        if (userContext.getAuthorities() == null || userContext.getAuthorities().isEmpty())
            throw new IllegalArgumentException("The login user has no privileges");

        Claims claims = Jwts.claims().setSubject(userContext.getUsername());
        claims.put(CLAIMS_SCOPES,
                userContext.getAuthorities().stream().map(Object::toString).collect(Collectors.toList()));

        LocalDateTime currentTime = LocalDateTime.now();

        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuer(appEnv.getTokenIssuer())
                .setIssuedAt(Date.from(currentTime.atZone(ZoneId.systemDefault()).toInstant()))
                .setExpiration(Date.from(currentTime
                        .plusMinutes(appEnv.getAccessTokenExpirationTime())
                        .atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(SignatureAlgorithm.HS512, appEnv.getTokenSigningKey())
                .compact();

        return new AccessToken(token, claims);
    }

    /**
     * This method creates a new refresh token with the given UserContext.
     *
     * @param userContext the user context
     * @return the refresh token
     * @throws IllegalArgumentException if the UserContext username is blank
     *
     * @see UserContext
     * @see JwtToken
     * @see Scopes
     */
    public JwtToken createRefreshToken(UserContext userContext) {
        if (StringUtils.isBlank(userContext.getUsername())) {
            throw new IllegalArgumentException("Username is required to create token.");
        }

        LocalDateTime currentTime = LocalDateTime.now();

        Claims claims = Jwts.claims().setSubject(userContext.getUsername());
        claims.put(CLAIMS_SCOPES, Arrays.asList(Scopes.REFRESH_TOKEN.authority()));

        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuer(appEnv.getTokenIssuer())
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(Date.from(currentTime.atZone(ZoneId.systemDefault()).toInstant()))
                .setExpiration(Date.from(currentTime
                        .plusMinutes(appEnv.getRefreshTokenExpirationTime())
                        .atZone(ZoneId.systemDefault()).toInstant()))
                .signWith(SignatureAlgorithm.HS512, appEnv.getTokenSigningKey())
                .compact();

        return new AccessToken(token, claims);
    }
}
