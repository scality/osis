package com.scality.osis.security.jwt.extractor;

public interface JwtTokenExtractor {
    String extract(String payload);
}
