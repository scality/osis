package com.scality.osis.utapiclient.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ListMetricsRequestDTOTest {

    @Test
    void listMetricsRequestValidation() {

        final ListMetricsRequestDTO listMetricsRequestDTO = ListMetricsRequestDTO.builder()
                .accounts(List.of("account1"))
                .timeRange(List.of(0L))
                .build();


        assertEquals(List.of("account1"), listMetricsRequestDTO.getAccounts(), "Invalid accounts");
        assertEquals(List.of(0L), listMetricsRequestDTO.getTimeRange(), "Invalid timeRange");
        assertNotNull(listMetricsRequestDTO.toString());
    }
}
