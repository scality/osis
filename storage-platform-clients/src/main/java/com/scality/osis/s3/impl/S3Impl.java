package com.scality.osis.s3.impl;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.amazonaws.util.StringUtils;
import com.scality.osis.s3.S3;
import okhttp3.HttpUrl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class S3Impl implements S3 {

    private final String s3Endpoint;

    /**
     * Create a s3 client implementation
     *
     * @param s3Endpoint s3 API endpoint
     */
    @Autowired
    public S3Impl(@Value("${osis.scality.s3.endpoint}") String s3Endpoint) {
        validEndpoint(s3Endpoint);
        this.s3Endpoint = s3Endpoint;
    }

    /**
     * Validate endpoint. If error throw exception
     *
     * @param endpoint s3 API endpoint
     */
    private static void validEndpoint(String endpoint) {
        if (HttpUrl.parse(endpoint) == null) {
            throw new IllegalArgumentException("endpoint is invalid");
        }
    }

    @Override
    public AmazonS3 getS3Client(Credentials credentials, String region) {
        if (StringUtils.isNullOrEmpty(credentials.getSessionToken())) {
            return AmazonS3ClientBuilder
                    .standard()
                    .withCredentials(
                            new AWSStaticCredentialsProvider(
                                    new BasicAWSCredentials(
                                            credentials.getAccessKeyId(),
                                            credentials.getSecretAccessKey())))
                    .withEndpointConfiguration(
                            new AwsClientBuilder.EndpointConfiguration(s3Endpoint, region))
                    .build();
        } else {
            return AmazonS3ClientBuilder
                    .standard()
                    .withCredentials(
                            new AWSStaticCredentialsProvider(
                                    new BasicSessionCredentials(
                                            credentials.getAccessKeyId(),
                                            credentials.getSecretAccessKey(),
                                            credentials.getSessionToken())))
                    .withEndpointConfiguration(
                            new AwsClientBuilder.EndpointConfiguration(s3Endpoint, region))
                    .build();
        }
    }
}
