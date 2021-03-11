package com.scality.osis.security.jwt.model;

public class RefreshTokenResponse {
    private String accessToken;

    public RefreshTokenResponse() {
    }

    public RefreshTokenResponse(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
