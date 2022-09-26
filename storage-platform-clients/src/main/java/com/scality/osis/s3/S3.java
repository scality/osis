package com.scality.osis.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.securitytoken.model.Credentials;

public interface S3 {
    AmazonS3 getS3Client(Credentials credentials, String region);
}
