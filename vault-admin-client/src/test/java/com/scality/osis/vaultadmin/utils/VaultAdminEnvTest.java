package com.scality.osis.vaultadmin.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.core.env.Environment;

import static com.scality.osis.vaultadmin.impl.cache.CacheConstants.DEFAULT_CACHE_MAX_CAPACITY;
import static com.scality.osis.vaultadmin.impl.cache.CacheConstants.ENV_LIST_ACCOUNT_DISABLED;
import static com.scality.osis.vaultadmin.impl.cache.CacheConstants.ENV_LIST_ACCOUNT_MAX_CAPACITY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class VaultAdminEnvTest {

    @Mock
    private Environment envMock;

    @InjectMocks
    private VaultAdminEnv vaultAdminEnv;

    @BeforeEach
    public void setup(){
        initMocks(this);
    }

    @Test
    public void testIsListAccountsCacheDisabled() {
        // Setup
        when(envMock.getProperty(ENV_LIST_ACCOUNT_DISABLED)).thenReturn(Boolean.FALSE.toString());

        // Run the test
        assertFalse(vaultAdminEnv.isListAccountsCacheDisabled());
    }

    @Test
    public void testGetListAccountsMaxCapacity() {
        // Setup
        when(envMock.getProperty(ENV_LIST_ACCOUNT_MAX_CAPACITY)).thenReturn(DEFAULT_CACHE_MAX_CAPACITY + "");

        // Run the test
        assertEquals(DEFAULT_CACHE_MAX_CAPACITY, vaultAdminEnv.getListAccountsMaxCapacity());
    }
}
