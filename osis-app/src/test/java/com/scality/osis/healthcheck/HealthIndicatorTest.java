/**
 * Copyright 2024 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.healthcheck;

import com.scality.osis.ScalityAppEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Exercises the actuator health indicators against unreachable endpoints so both
 * the down path (connection fails) and the construction path are covered without a
 * real S3/Vault. A free, immediately-refused port keeps the test fast and offline.
 */
class HealthIndicatorTest {

    // 1 is a reserved port that connection attempts refuse immediately.
    private static final String UNREACHABLE = "http://127.0.0.1:1";

    @Mock
    private ScalityAppEnv appEnv;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void s3HealthIsDownWhenEndpointUnreachable() {
        when(appEnv.getS3Endpoint()).thenReturn(UNREACHABLE);
        when(appEnv.getS3HealthCheckTimeout()).thenReturn(100);

        final S3HealthIndicator indicator = new S3HealthIndicator();
        ReflectionTestUtils.setField(indicator, "appEnv", appEnv);

        final Health health = indicator.health();
        assertEquals(Status.DOWN, health.getStatus());
    }

    @Test
    void vaultHealthIsDownWhenEndpointUnreachable() {
        when(appEnv.getPlatformEndpoint()).thenReturn(UNREACHABLE);
        when(appEnv.getVaultHealthCheckTimeout()).thenReturn(100);

        final VaultHealthIndicator indicator = new VaultHealthIndicator();
        ReflectionTestUtils.setField(indicator, "appEnv", appEnv);

        final Health health = indicator.health();
        assertEquals(Status.DOWN, health.getStatus());
    }
}
