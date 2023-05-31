package com.scality.osis.utapiclient.dto;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ListMetricsRequestDTO extends com.amazonaws.AmazonWebServiceRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 7894667787877184061L;

    private List<String> buckets;

    private List<String> accounts;

    private List<String> users;

    private List<String> service;

    private List<Long> timeRange;
}
