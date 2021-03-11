package com.scality.osis.security.jwt.verifier;

import org.springframework.stereotype.Component;

@Component
public class DummyFilterTokenVerifier implements JwtTokenVerifier {
    @Override
    public boolean verify(String jti) {
        return true;
    }
}
