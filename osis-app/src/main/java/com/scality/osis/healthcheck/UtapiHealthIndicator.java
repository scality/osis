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

@Component("utapi")
public class UtapiHealthIndicator implements HealthIndicator {
    private static final Logger logger = LoggerFactory.getLogger(UtapiHealthIndicator.class);

    @Autowired
    private ScalityAppEnv appEnv;

    @Override
    public Health health() {
        HttpURLConnection connection = null;
        try {
            // check if Utapi Endpoint is reachable
            connection = (HttpURLConnection) new java.net.URL(appEnv.getUtapiEndpoint()).openConnection();
            connection.setConnectTimeout(appEnv.getUtapiHealthCheckTimeout());
            connection.connect();
        } catch (SocketTimeoutException e) {
            logger.warn("Failed to connect to Utapi endpoint {} in timeout {}",appEnv.getUtapiEndpoint(), appEnv.getUtapiHealthCheckTimeout());
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        } catch (IOException e) {
            logger.warn("an I/O error occurs while trying to connect {}.",appEnv.getUtapiEndpoint());
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
