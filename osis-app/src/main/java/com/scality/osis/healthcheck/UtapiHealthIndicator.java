/**
 *Copyright 2021 Scality, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.healthcheck;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;


@Component("utapi")
public class UtapiHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        // We want to make sure healthcheck is false even if it is set in application.properties
        // This will be enabled and potentially refactored with S3C-8266
        return Health.down().build();
    }
}
