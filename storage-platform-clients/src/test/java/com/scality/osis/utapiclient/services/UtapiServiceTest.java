package com.scality.osis.utapiclient.services;

import com.amazonaws.Request;
import com.amazonaws.Response;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.http.AmazonHttpClient;
import com.scality.osis.utapiclient.dto.ListMetricsRequestDTO;
import com.scality.osis.utapiclient.dto.MetricsData;
import com.scality.osis.utapiclient.utils.UtapiClientException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class UtapiServiceTest {

    // mock utapi client
    private static AmazonHttpClient listMetricsAmazonHttpClient;

    // dummy Aws credentials
    private static BasicAWSCredentials basicAWSCredentials;

    // Utapi Service
    private static UtapiServiceClient utapiServiceMockClient;

    private static ListMetricsRequestDTO listMetricsRequestDTO;

    @BeforeAll
    static void init() {
        basicAWSCredentials = new BasicAWSCredentials("accesskey", "secretkey");
        initListMetricsMocks();
    }

    /* default */ static void initListMetricsMocks() {

        listMetricsRequestDTO = ListMetricsRequestDTO.builder()
                .accounts(List.of("account1"))
                .timeRange(List.of(0L))
                .build();

        listMetricsAmazonHttpClient = mock(AmazonHttpClient.class);
        utapiServiceMockClient = new UtapiServiceClient(listMetricsAmazonHttpClient, basicAWSCredentials);

        //Default list metrics mock response
        when(listMetricsAmazonHttpClient.execute(any(Request.class), any(), any(), any(),any()))
                .thenAnswer(new Answer<Response>() {
                    @Override
                    public Response<MetricsData[]> answer(final InvocationOnMock invocation) {

                        final MetricsData data = new MetricsData();
                        data.setAccountId("account1");
                        data.setIncomingBytes(0L);
                        data.setOutgoingBytes(0L);
                        data.setNumberOfObjects(Arrays.asList(0L, 0L));
                        data.setStorageUtilized(Arrays.asList(0L, 0L));
                        data.setOperations(Map.of("s3:ListObject", 0));
                        data.setTimeRange(List.of(0L));

                        MetricsData[] metricsData = new MetricsData[1];
                        metricsData[0] = data;

                        return new Response<>(metricsData,null);
                    }
                });

        utapiServiceMockClient = new UtapiServiceClient(listMetricsAmazonHttpClient, basicAWSCredentials);
    }

    @Test
    void testListMetrics() {

        final MetricsData[] response = utapiServiceMockClient.listAccountsMetrics(listMetricsRequestDTO).getAwsResponse();

        assertEquals("account1", response[0].getAccountId(), "Account Id is not matching");
        assertEquals(0L, response[0].getIncomingBytes(), "Incoming bytes is not matching");
        assertEquals(0L, response[0].getOutgoingBytes(), "Outgoing bytes is not matching");
        assertEquals(Arrays.asList(0L, 0L), response[0].getNumberOfObjects(), "Number of objects is not matching");
        assertEquals(Arrays.asList(0L, 0L), response[0].getStorageUtilized(), "Storage utilized is not matching");
        assertEquals(Map.of("s3:ListObject", 0), response[0].getOperations(), "Operations is not matching");
        assertEquals(List.of(0L), response[0].getTimeRange(), "Time range is not matching");
    }

    @Test
    void testListMetricsWithAccessDeniedException() {

        when(listMetricsAmazonHttpClient.execute(any(Request.class), any(), any(), any(),any()))
                .thenAnswer(new Answer<Response>() {
                    @Override
                    public Response<MetricsData[]> answer(final InvocationOnMock invocation) {
                        final UtapiClientException error = new UtapiClientException("Access Denied");
                        error.setErrorCode("Forbidden");
                        error.setStatusCode(403);
                        throw error;
                    }
                });

        final UtapiClientException error = assertThrows(UtapiClientException.class, () -> {
            utapiServiceMockClient.listAccountsMetrics(listMetricsRequestDTO);
        }, "Expected UtapiClientException");
        assertEquals(403, error.getStatusCode(), "Expected http status code: 403");
        assertEquals("Forbidden", error.getErrorCode(), "Expected error code: Forbidden");
        assertEquals("Access Denied", error.getErrorMessage(), "Invalid error message");

        //reinit the amazonHttpClient
        init();
    }

    @Test
    void testListMetricsWithInvalidRequestException() {

        when(listMetricsAmazonHttpClient.execute(any(Request.class), any(), any(), any(),any()))
                .thenAnswer(new Answer<Response>() {
                    @Override
                    public Response<MetricsData[]> answer(final InvocationOnMock invocation) {
                        final UtapiClientException error = new UtapiClientException("Request validation error");
                        error.setErrorCode("InvalidRequest");
                        error.setStatusCode(400);
                        throw error;
                    }
                });

        final UtapiClientException error = assertThrows(UtapiClientException.class, () -> {
            utapiServiceMockClient.listAccountsMetrics(listMetricsRequestDTO);
        }, "Expected UtapiClientException");
        assertEquals(400, error.getStatusCode(), "Expected http status code: 403");
        assertEquals("InvalidRequest", error.getErrorCode(), "Expected error code: Forbidden");
        assertEquals("Request validation error", error.getErrorMessage(), "Invalid error message");

        //reinit the amazonHttpClient
        init();
    }
}
