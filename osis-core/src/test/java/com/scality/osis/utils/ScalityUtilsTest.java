package com.scality.osis.utils;

import com.scality.vaultclient.dto.AccountData;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.scality.osis.utils.ScalityConstants.*;
import static com.scality.osis.utils.ScalityTestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class ScalityUtilsTest {

    @Test
    public void testIsValidUUID() {
        assertThat(ScalityUtils.isValidUUID(UUID.randomUUID().toString())).isTrue();
    }

    @Test
    public void testIsInValidUUID() {
        assertThat(ScalityUtils.isValidUUID("str")).isFalse();
    }

    @Test
    public void testParseFilter() {
        // Setup
        final String filter = TENANT_ID_PREFIX + SAMPLE_TENANT_ID + FILTER_SEPARATOR + USER_ID_PREFIX + TEST_USER_ID ;

        // Run the test
        final Map<String, String> filterMap = ScalityUtils.parseFilter(filter);

        // Verify the results
        assertEquals(SAMPLE_TENANT_ID, filterMap.get(OSIS_TENANT_ID));
        assertEquals(TEST_USER_ID, filterMap.get(OSIS_USER_ID));
    }

    @Test
    public void testParseEmptyFilter() {
        // Setup

        // Run the test
        final Map<String, String> filterMap = ScalityUtils.parseFilter("");

        // Verify the results
        assertTrue(filterMap.isEmpty());
    }

    @Test
    public void testVaultAccountContainsCdTenantIdsTrue() {
        // Setup
        final AccountData accountData = new AccountData();
        final Map<String, String> customAttributes = new HashMap<>();
        customAttributes.put(CD_TENANT_ID_PREFIX + SAMPLE_CD_TENANT_ID, "");
        accountData.setId(SAMPLE_TENANT_ID);
        accountData.setCustomAttributes(customAttributes);

        // Run the test
        final boolean result = ScalityUtils.vaultAccountContainsCdTenantIds(accountData);

        // Verify the results
        assertTrue(result);
    }

    @Test
    public void testVaultAccountContainsCdTenantIdsFalse() {
        // Setup
        final AccountData accountData = new AccountData();
        final Map<String, String> customAttributes = new HashMap<>();
        accountData.setId(SAMPLE_TENANT_ID);
        accountData.setCustomAttributes(customAttributes);

        // Run the test
        final boolean result = ScalityUtils.vaultAccountContainsCdTenantIds(accountData);

        // Verify the results
        assertFalse(result);
    }
}
