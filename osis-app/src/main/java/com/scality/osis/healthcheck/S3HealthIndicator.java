/**
 *Copyright 2021 Scality, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.healthcheck;

import com.scality.osis.ScalityAppEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;

@Component("s3")
public class S3HealthIndicator implements HealthIndicator {
    private static final Logger logger = LoggerFactory.getLogger(S3HealthIndicator.class);

    @Autowired
    private ScalityAppEnv appEnv;

    @Override
    public Health health() {
        HttpURLConnection connection = null;
        try {
            // check if S3 Endpoint is reachable
            connection = (HttpURLConnection) new java.net.URL(appEnv.getS3Endpoint()).openConnection();
            connection.setConnectTimeout(appEnv.getS3HealthCheckTimeout());
            connection.connect();
        } catch (SocketTimeoutException e) {
            logger.warn("Failed to connect to S3 endpoint {} in timeout {}",appEnv.getS3Endpoint(), appEnv.getS3HealthCheckTimeout());
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        } catch (IOException e) {
            logger.warn("an I/O error occurs while trying to connect {}.",appEnv.getS3Endpoint());
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return Health.up().build();
    }
}
