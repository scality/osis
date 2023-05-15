package com.scality.osis.utapiclient.dto;

import com.amazonaws.Request;
import com.amazonaws.SdkClientException;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.protocol.OperationInfo;
import com.amazonaws.protocol.Protocol;
import com.amazonaws.protocol.ProtocolRequestMarshaller;
import com.amazonaws.protocol.json.SdkJsonProtocolFactory;
import com.amazonaws.transform.Marshaller;

/**
 * A GenericRequestProtocolMarshaller that implements Marshaller
 */
@SuppressWarnings("unchecked")
public class GenericRequestProtocolMarshaller<T> implements Marshaller<Request<T>,T> {
    private final SdkJsonProtocolFactory protocolFactory;

    private final GenericRequestMarshaller requestMarshaller;

    private final OperationInfo sdkOperationBinding;

    public GenericRequestProtocolMarshaller(SdkJsonProtocolFactory protocolFactory,
                                            GenericRequestMarshaller requestMarshaller,
                                            String requestUriPath) {
        this.protocolFactory = protocolFactory;
        this.requestMarshaller = requestMarshaller;
        this.sdkOperationBinding = OperationInfo.builder().protocol(Protocol.AWS_JSON).requestUri(requestUriPath)
                .httpMethodName(HttpMethodName.POST).hasExplicitPayloadMember(false).hasPayloadMembers(false).serviceName("s3").build();
    }

    /**
     * Instantiates protocolMarshaller and marshals request dto with bindings in to Amazon Web Service Request object
     *
     * @param request   the request dto
     * @return Returns the Amazon Web Service Request object
     */
    @Override
    public Request<T> marshall(T request) {

        if (request == null) {
            throw new SdkClientException("Invalid argument passed to marshall");
        }

        try {
            final ProtocolRequestMarshaller<T> protocolMarshaller = protocolFactory.createProtocolMarshaller(sdkOperationBinding,
                    request);

            protocolMarshaller.startMarshalling();
            requestMarshaller.marshall(request, protocolMarshaller);
            return protocolMarshaller.finishMarshalling();
        } catch (Exception e) {
            throw new SdkClientException("Unable to marshall request to JSON: " + e.getMessage(), e);
        }
    }

    public SdkJsonProtocolFactory getProtocolFactory() {
        return protocolFactory;
    }
}
