package com.scality.osis.security.jwt.verifier;

public interface JwtTokenVerifier {
    boolean verify(String jti);
}
