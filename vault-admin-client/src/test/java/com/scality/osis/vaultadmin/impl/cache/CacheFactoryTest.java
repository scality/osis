package com.scality.osis.vaultadmin.impl.cache;

import com.scality.osis.vaultadmin.utils.VaultAdminEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static com.scality.osis.vaultadmin.impl.cache.CacheConstants.DEFAULT_CACHE_MAX_CAPACITY;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static com.scality.osis.vaultadmin.impl.cache.CacheConstants.NAME_LIST_ACCOUNTS_CACHE;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CacheFactoryTest {

    private CacheFactory cacheFactoryUnderTest;

    @Mock
    private VaultAdminEnv envMock;

    @BeforeEach
    public void setUp() {

        initMocks();
        cacheFactoryUnderTest = new CacheFactory(envMock);
    }

    private void initMocks() {

        envMock = Mockito.mock(VaultAdminEnv.class);
        // Setup
        when(envMock.getListAccountsMaxCapacity()).thenReturn(DEFAULT_CACHE_MAX_CAPACITY);
        when(envMock.isListAccountsCacheDisabled()).thenReturn(false);
    }

    @Test
    public void testGetCache() {
        // Setup

        // Run the test
        final Cache result = cacheFactoryUnderTest.getCache(NAME_LIST_ACCOUNTS_CACHE);

        // Verify the results
        assertNotNull(result);
    }

    @Test
    public void testGetCacheInvalidValue() {
        // Setup

        // Run the test
        final Cache result = cacheFactoryUnderTest.getCache("dummy");

        // Verify the results
        assertNull(result);
    }
}
