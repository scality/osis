package com.scality.osis.utapi.impl;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.amazonaws.util.StringUtils;
import com.scality.osis.utapi.Utapi;
import com.scality.osis.utapiclient.services.UtapiServiceClient;
import okhttp3.HttpUrl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@SuppressWarnings({"deprecation"})
@Component
public class UtapiImpl implements Utapi {
    private UtapiServiceClient utapiService;
    private final String utapiEndpoint;

    @Autowired
    public UtapiImpl(@Value("${osis.scality.utapi.endpoint}") String utapiEndpoint) {
        validEndpoint(utapiEndpoint);
        this.utapiEndpoint = utapiEndpoint;
    }

    public UtapiImpl(UtapiServiceClient utapiServiceClient,
                     String utapiEndpoint, String utapiEndpoint1) {
        this.utapiEndpoint = utapiEndpoint1;

        validEndpoint(utapiEndpoint);
        this.utapiService = utapiServiceClient;
        utapiService.setEndpoint(utapiEndpoint);
    }

    /**
     * Validate endpoint. If error throw exception
     * @param endpoint Utapi API endpoint, e.g., http://127.0.0.1:8100
     */
    private static void validEndpoint(String endpoint) {
        if (HttpUrl.parse(endpoint) == null) {
            throw new IllegalArgumentException("utapi endpoint is invalid");
        }
    }

    @Override
    public UtapiServiceClient getUtapiServiceClient(Credentials credentials, String region) {
        if (StringUtils.isNullOrEmpty(credentials.getSessionToken())) {
            utapiService = new UtapiServiceClient(
                    new BasicAWSCredentials(credentials.getAccessKeyId(), credentials.getSecretAccessKey()));
        } else {
            utapiService = new UtapiServiceClient(
                    new BasicSessionCredentials(credentials.getAccessKeyId(), credentials.getSecretAccessKey(),
                            credentials.getSessionToken()));
        }
        utapiService.setEndpoint(utapiEndpoint);
        return utapiService;
    }
}
