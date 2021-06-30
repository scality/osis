package com.scality.osis.utils;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class ScalityUtilsTest {

    @Test
    public void testIsValidUUID() {
        assertThat(ScalityUtils.isValidUUID(UUID.randomUUID().toString())).isTrue();
    }

    @Test
    public void testIsInValidUUID() {
        assertThat(ScalityUtils.isValidUUID("str")).isFalse();
    }
}
