package com.scality.osis.utapiclient.services;

import com.amazonaws.Response;
import com.scality.osis.utapiclient.dto.ListMetricsRequestDTO;
import com.scality.osis.utapiclient.dto.MetricsData;

public interface UtapiService {
    Response<MetricsData[]> listAccountsMetrics(ListMetricsRequestDTO listMetricsRequestDTO);

    Response<MetricsData[]> listUsersMetrics(ListMetricsRequestDTO listMetricsRequestDTO);

    Response<MetricsData[]> listBucketsMetrics(ListMetricsRequestDTO listMetricsRequestDTO);
}
