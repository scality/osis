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
import java.net.HttpURLConnection;

@Component("vault")
public class VaultHealthIndicator implements HealthIndicator {
    private static final Logger logger = LoggerFactory.getLogger(VaultHealthIndicator.class);

    @Autowired
    private ScalityAppEnv appEnv;

    @Override
    public Health health() {
        HttpURLConnection connection = null;
        try {
            // check if Vault Admin Endpoint is reachable
            connection = (HttpURLConnection) new java.net.URL(appEnv.getPlatformEndpoint()).openConnection();
            connection.setConnectTimeout(appEnv.getVaultHealthCheckTimeout());
            connection.connect();

            // check if Vault S3 Interface Endpoint is reachable
            connection = (HttpURLConnection) new java.net.URL(appEnv.getS3InterfaceEndpoint()).openConnection();
            connection.setConnectTimeout(appEnv.getVaultHealthCheckTimeout());
            connection.connect();
        } catch (Exception e) {
            logger.warn("Failed to connect to Vault endpoint: {}",appEnv.getPlatformEndpoint());
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
