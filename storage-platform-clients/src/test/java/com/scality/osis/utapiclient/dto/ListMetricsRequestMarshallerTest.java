package com.scality.osis.utapiclient.dto;

import com.amazonaws.protocol.ProtocolRequestMarshaller;
import com.scality.osis.utapiclient.utils.UtapiClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ListMetricsRequestMarshallerTest {

    private ListMetricsRequestMarshaller listMetricsRequestMarshallerUnderTest;

    @BeforeEach
    public void setUp() {
        listMetricsRequestMarshallerUnderTest = new ListMetricsRequestMarshaller();
    }

    @Test
    void testMarshallNoResource() {
        // Setup
        final ListMetricsRequestDTO request = ListMetricsRequestDTO.builder()
                .timeRange(List.of(0L))
                .build();

        final ProtocolRequestMarshaller<ListMetricsRequestDTO> protocolMarshaller = mock(ProtocolRequestMarshaller.class);
        doNothing().when(protocolMarshaller).marshall(any(), any());

        // Verify the results
        final UtapiClientException error = assertThrows(UtapiClientException.class, () -> {
            listMetricsRequestMarshallerUnderTest.marshall(request, protocolMarshaller);
        }, "Expected UtapiClientException");

        assertTrue(error.getErrorMessage().contains("no resource specified"), "Invalid error message");
    }

    @Test
    void testMarshallMoreThanOneResource() {
        // Setup
        final ListMetricsRequestDTO request = ListMetricsRequestDTO.builder()
                .accounts(List.of("account1"))
                .buckets(List.of("bucket1"))
                .timeRange(List.of(0L))
                .build();

        final ProtocolRequestMarshaller<ListMetricsRequestDTO> protocolMarshaller = mock(ProtocolRequestMarshaller.class);
        doNothing().when(protocolMarshaller).marshall(any(), any());

        // Verify the results
        final UtapiClientException error = assertThrows(UtapiClientException.class, () -> {
            listMetricsRequestMarshallerUnderTest.marshall(request, protocolMarshaller);
        }, "Expected UtapiClientException");

        assertTrue(error.getErrorMessage().contains("only one resource can be specified"), "Invalid error message");
    }

    @Test
    void testMarshallNoTimeRange() {
        // Setup
        final ListMetricsRequestDTO request = ListMetricsRequestDTO.builder()
                .accounts(List.of("account1"))
                .build();

        final ProtocolRequestMarshaller<ListMetricsRequestDTO> protocolMarshaller = mock(ProtocolRequestMarshaller.class);
        doNothing().when(protocolMarshaller).marshall(any(), any());

        // Verify the results
        final UtapiClientException error = assertThrows(UtapiClientException.class, () -> {
            listMetricsRequestMarshallerUnderTest.marshall(request, protocolMarshaller);
        }, "Expected UtapiClientException");

        assertTrue(error.getErrorMessage().contains("timeRange is required"), "Invalid error message");
    }
}
