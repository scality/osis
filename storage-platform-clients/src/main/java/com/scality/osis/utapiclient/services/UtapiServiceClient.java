package com.scality.osis.utapiclient.services;

import com.amazonaws.Response;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.http.AmazonHttpClient;
import com.scality.osis.utapiclient.dto.ListMetricsRequestDTO;
import com.scality.osis.utapiclient.dto.ListMetricsRequestMarshaller;
import com.scality.osis.utapiclient.dto.MetricsData;


public class UtapiServiceClient extends BaseServicesClient implements UtapiService {

    public UtapiServiceClient(AWSCredentials awsCredentials) {
        super(awsCredentials);
    }

    /**
     * Constructs a new client to invoke service methods on IAM. A credentials provider chain will be used that searches
     * for credentials in this order:
     * <ul>
     * <li>Environment Variables - AWS_ACCESS_KEY_ID and AWS_SECRET_KEY</li>
     * <li>Java System Properties - aws.accessKeyId and aws.secretKey</li>
     * <li>Instance profile credentials delivered through the Amazon EC2 metadata service</li>
     * </ul>
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not return until the service call
     * completes.
     *  @param client         *        client object.
     *
     * @param awsCredentials *        the aws credentials
     */
    public UtapiServiceClient(AmazonHttpClient client, AWSCredentials awsCredentials) {
        super(client, awsCredentials);
    }

    @Override
    public Response<MetricsData[]> listAccountsMetrics(ListMetricsRequestDTO listMetricsRequestDTO) {
        return execute(listMetricsRequestDTO, "ListMetrics", "/accounts?Action=ListMetrics",
                ListMetricsRequestMarshaller.getInstance(), MetricsData[].class);
    }

    @Override
    public Response<MetricsData[]> listUsersMetrics(ListMetricsRequestDTO listMetricsRequestDTO) {
        return execute(listMetricsRequestDTO, "ListMetrics", "/users?Action=ListMetrics",
                ListMetricsRequestMarshaller.getInstance(), MetricsData[].class);
    }

    @Override
    public Response<MetricsData[]> listBucketsMetrics(ListMetricsRequestDTO listMetricsRequestDTO) {
        return execute(listMetricsRequestDTO, "ListMetrics", "/buckets?Action=ListMetrics",
                ListMetricsRequestMarshaller.getInstance(), MetricsData[].class);
    }
}
