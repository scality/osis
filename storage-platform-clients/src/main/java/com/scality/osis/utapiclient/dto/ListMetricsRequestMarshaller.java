package com.scality.osis.utapiclient.dto;

import com.amazonaws.protocol.MarshallLocation;
import com.amazonaws.protocol.MarshallingInfo;
import com.amazonaws.protocol.MarshallingType;
import com.amazonaws.protocol.ProtocolRequestMarshaller;
import com.scality.osis.utapiclient.utils.UtapiClientException;

import java.util.List;


public class ListMetricsRequestMarshaller extends GenericRequestMarshaller<ListMetricsRequestDTO> {
    private static final MarshallingInfo<List> BUCKETS_BINDING = MarshallingInfo.builder(MarshallingType.LIST).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("buckets").build();
    private static final MarshallingInfo<List> ACCOUNTS_BINDING = MarshallingInfo.builder(MarshallingType.LIST).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("accounts").build();
    private static final MarshallingInfo<List> USERS_BINDING = MarshallingInfo.builder(MarshallingType.LIST).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("users").build();
    private static final MarshallingInfo<List> SERVICE_BINDING = MarshallingInfo.builder(MarshallingType.LIST).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("service").build();
    private static final MarshallingInfo<List> TIMERANGE_BINDING = MarshallingInfo.builder(MarshallingType.LIST).marshallLocation(MarshallLocation.PAYLOAD)
            .marshallLocationName("timeRange").build();


    private static final String LIST_METRICS_ACTION = "ListMetrics";

    private static final ListMetricsRequestMarshaller instance = new ListMetricsRequestMarshaller();

    public static ListMetricsRequestMarshaller getInstance() {
        return instance;
    }

    @Override
    public void marshall(ListMetricsRequestDTO listMetricsRequestDTO, ProtocolRequestMarshaller<ListMetricsRequestDTO> protocolMarshaller) {
        super.marshall(listMetricsRequestDTO, protocolMarshaller, LIST_METRICS_ACTION);
        try {
            int nonEmptyResourceCount = 0;
            if (listMetricsRequestDTO.getBuckets() != null && !listMetricsRequestDTO.getBuckets().isEmpty()) {
                nonEmptyResourceCount++;
                protocolMarshaller.marshall(listMetricsRequestDTO.getBuckets(), BUCKETS_BINDING);
            }
            if (listMetricsRequestDTO.getAccounts() != null && !listMetricsRequestDTO.getAccounts().isEmpty()) {
                nonEmptyResourceCount++;
                protocolMarshaller.marshall(listMetricsRequestDTO.getAccounts(), ACCOUNTS_BINDING);
            }
            if (listMetricsRequestDTO.getUsers() != null && !listMetricsRequestDTO.getUsers().isEmpty()) {
                nonEmptyResourceCount++;
                protocolMarshaller.marshall(listMetricsRequestDTO.getUsers(), USERS_BINDING);
            }
            if (listMetricsRequestDTO.getService() != null && !listMetricsRequestDTO.getService().isEmpty()) {
                nonEmptyResourceCount++;
                protocolMarshaller.marshall(listMetricsRequestDTO.getService(), SERVICE_BINDING);
            }
            if (nonEmptyResourceCount == 0) {
                throw new UtapiClientException("no resource specified");
            }
            if (nonEmptyResourceCount > 1) {
                throw new UtapiClientException("only one resource can be specified");
            }

            if (listMetricsRequestDTO.getTimeRange() == null || listMetricsRequestDTO.getTimeRange().isEmpty()) {
                throw new UtapiClientException("timeRange is required");
            }
            protocolMarshaller.marshall(listMetricsRequestDTO.getTimeRange(), TIMERANGE_BINDING);

        } catch (Exception e) {
            throw new UtapiClientException("Unable to marshall request to JSON: " + e.getMessage(), e);
        }
    }
}
