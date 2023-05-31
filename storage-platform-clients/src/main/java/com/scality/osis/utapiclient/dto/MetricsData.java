package com.scality.osis.utapiclient.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class MetricsData {
    String accountId;

    String bucketName;

    List<Long> timeRange;

    Map<String, Integer> operations;

    Long incomingBytes;

    Long outgoingBytes;

    List<Long> numberOfObjects;

    List<Long> storageUtilized;
}
