package com.scality.osis.vaultadmin.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.env.Environment;

import static com.scality.osis.vaultadmin.impl.cache.CacheConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

class VaultAdminEnvTest {

    @Mock
    private Environment envMock;

    @InjectMocks
    private VaultAdminEnv vaultAdminEnv;

    @BeforeEach
    public void setup(){
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testIsListAccountsCacheDisabled() {
        // Setup
        when(envMock.getProperty(ENV_LIST_ACCOUNT_DISABLED)).thenReturn(Boolean.FALSE.toString());

        // Run the test
        assertFalse(vaultAdminEnv.isListAccountsCacheDisabled());
    }

    @Test
    void testGetListAccountsMaxCapacity() {
        // Setup
        when(envMock.getProperty(ENV_LIST_ACCOUNT_MAX_CAPACITY)).thenReturn(DEFAULT_CACHE_MAX_CAPACITY + "");

        // Run the test
        assertEquals(DEFAULT_CACHE_MAX_CAPACITY, vaultAdminEnv.getListAccountsCacheMaxCapacity());
    }

    @Test
    void testGetListAccountsExpiration() {
        // Setup
        when(envMock.getProperty(ENV_LIST_ACCOUNT_CACHE_TTL)).thenReturn((int) DEFAULT_CACHE_TTL_IN_MS + "");

        // Run the test
        assertEquals((int) DEFAULT_CACHE_TTL_IN_MS, vaultAdminEnv.getListAccountsCacheExpiration());
    }

    @Test
    void testGetAssumeRoleCacheMaxCapacity() {
        // Setup
        when(envMock.getProperty(ENV_ASSUME_ROLE_MAX_CAPACITY)).thenReturn(DEFAULT_CACHE_MAX_CAPACITY + "");

        // Run the test
        assertEquals(DEFAULT_CACHE_MAX_CAPACITY, vaultAdminEnv.getAssumeRoleCacheMaxCapacity());
    }

    @Test
    void testGetAssumeRoleCacheExpiration() {
        // Setup
        when(envMock.getProperty(ENV_ASSUME_ROLE_CACHE_TTL)).thenReturn((int) DEFAULT_CACHE_TTL_IN_MS + "");

        // Run the test
        assertEquals((int) DEFAULT_CACHE_TTL_IN_MS, vaultAdminEnv.getAssumeRoleCacheExpiration());
    }
}
