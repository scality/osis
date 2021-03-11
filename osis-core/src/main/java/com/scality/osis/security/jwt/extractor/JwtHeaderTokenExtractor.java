package com.scality.osis.security.jwt.extractor;

import com.scality.osis.security.jwt.AuthConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationServiceException;

public class JwtHeaderTokenExtractor implements JwtTokenExtractor {

    @Override
    public String extract(String header) {
        if (StringUtils.isBlank(header)) {
            throw new AuthenticationServiceException("Authorization header cannot be blank!");
        }

        if (header.length() < AuthConstants.HEADER_PREFIX.length()) {
            throw new AuthenticationServiceException("Invalid authorization header size.");
        }

        return header.substring(AuthConstants.HEADER_PREFIX.length());
    }
}
