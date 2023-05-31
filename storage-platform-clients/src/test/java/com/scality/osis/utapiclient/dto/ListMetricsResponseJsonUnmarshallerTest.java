package com.scality.osis.utapiclient.dto;

import com.amazonaws.http.HttpResponse;
import com.amazonaws.transform.JsonUnmarshallerContext;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("PMD.LoosePackageCoupling")
class ListMetricsResponseJsonUnmarshallerTest {

    private GenericResponseJsonUnmarshaller<MetricsData[]> listMetricsResponseJsonUnmarshallerUnderTest;

    @BeforeEach
    void setUp() {
        listMetricsResponseJsonUnmarshallerUnderTest = new GenericResponseJsonUnmarshaller<>(MetricsData[].class);
    }

    @Test
    void testUnmarshall() throws Exception {
        // Setup
        final MetricsData[] expectedResult = new MetricsData[1];
        final MetricsData data = new MetricsData();
        data.setAccountId("accountId");
        data.setIncomingBytes(0L);
        data.setOutgoingBytes(0L);
        data.setNumberOfObjects(Arrays.asList(0L, 0L));
        data.setStorageUtilized(Arrays.asList(0L, 0L));
        data.setOperations(Map.of("s3:ListObject", 0));
        data.setTimeRange(List.of(0L));

        expectedResult[0] = data;

        final JsonUnmarshallerContext context = mock(JsonUnmarshallerContext.class);
        when(context.getHttpResponse())
                .thenAnswer(new Answer<HttpResponse>() {
                    @Override
                    public HttpResponse answer(final InvocationOnMock invocation) {
                        final HttpResponse httpsResponse = new HttpResponse(null, null);
                        httpsResponse.setContent(new ByteArrayInputStream(new Gson().toJson(expectedResult).getBytes(StandardCharsets.UTF_8)));
                        return httpsResponse;
                    }
                });

        // Run the test
        final MetricsData[] result = listMetricsResponseJsonUnmarshallerUnderTest.unmarshall(context);

        // Verify the results
        assertEquals(expectedResult[0].accountId, result[0].getAccountId());
        assertEquals(expectedResult[0].incomingBytes, result[0].getIncomingBytes());
        assertEquals(expectedResult[0].outgoingBytes, result[0].getOutgoingBytes());
        assertEquals(expectedResult[0].numberOfObjects, result[0].getNumberOfObjects());
        assertEquals(expectedResult[0].storageUtilized, result[0].getStorageUtilized());
        assertEquals(expectedResult[0].operations, result[0].getOperations());
        assertEquals(expectedResult[0].timeRange, result[0].getTimeRange());
    }

    @Test
    void testUnmarshallThrowsException() {
        // Setup
        final JsonUnmarshallerContext context = null;

        // Run the test
        assertThrows(Exception.class, () -> listMetricsResponseJsonUnmarshallerUnderTest.unmarshall(context));
    }
}
