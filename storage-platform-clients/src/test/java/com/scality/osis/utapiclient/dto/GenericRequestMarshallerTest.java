package com.scality.osis.utapiclient.dto;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.protocol.ProtocolMarshaller;
import com.scality.osis.utapiclient.utils.UtapiClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GenericRequestMarshallerTest {

    private GenericRequestMarshaller genericRequestMarshallerUnderTest;

    @BeforeEach
    public void setUp() {
        genericRequestMarshallerUnderTest = new ListMetricsRequestMarshaller();
    }

    @Test
    void testMarshallNullRequest() {
        // Setup
        final AmazonWebServiceRequest request = null;
        final ProtocolMarshaller protocolMarshaller = null;

        // Verify the results
        final UtapiClientException error = assertThrows(UtapiClientException.class, () -> {
            genericRequestMarshallerUnderTest.marshall(request, protocolMarshaller, "action");
        }, "Expected UtapiClientException");

        assertEquals("Invalid argument passed to marshall.", error.getErrorMessage(), "Invalid error message");
    }

    @Test
    void testMarshallNullMarshaller() {
        // Setup
        final AmazonWebServiceRequest request = new AmazonWebServiceRequest() {
        };
        final ProtocolMarshaller protocolMarshaller = null;

        // Verify the results
        final UtapiClientException error = assertThrows(UtapiClientException.class, () -> {
            genericRequestMarshallerUnderTest.marshall(request, protocolMarshaller, "action");
        }, "Expected UtapiClientException");
        assertTrue(error.getErrorMessage().startsWith("Unable to marshall request to JSON: "), "Invalid error message");
    }
}
