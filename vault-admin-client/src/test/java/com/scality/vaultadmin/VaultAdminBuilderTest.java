package com.scality.vaultadmin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class VaultAdminBuilderTest {

    private VaultAdminBuilder vaultAdminBuilderUnderTest;
    private static final String TEST_END_POINT = "http://127.0.0.1:8080";
    private static final String TEST_ACCESS_KEY = "test.access.key";
    private static final String TEST_SECRET_KEY = "test.secret.key";

    @BeforeEach
    public void setUp() {
        vaultAdminBuilderUnderTest = new VaultAdminBuilder();
        vaultAdminBuilderUnderTest.setEndpoint(TEST_END_POINT);
        vaultAdminBuilderUnderTest.setAccessKey(TEST_ACCESS_KEY);
        vaultAdminBuilderUnderTest.setSecretKey(TEST_SECRET_KEY);
    }

    @Test
    public void testBuild() {
        // Setup


        // Run the test
        final VaultAdmin result = vaultAdminBuilderUnderTest.build();

        // Verify the results
        assertNotNull(result.getVaultAccountclient(), "VaultAdminBuilder.build() should create VaultAccountclient object");

    }

    @Test
    public void testBuildException() {
        // Setup with not setting required parameters
        vaultAdminBuilderUnderTest = new VaultAdminBuilder();

        // Run Test & Verify the results
        assertThrows(IllegalArgumentException.class,
                () -> vaultAdminBuilderUnderTest.build(), "VaultAdminBuilder.build() should throw IllegalArgumentException");

    }
}
