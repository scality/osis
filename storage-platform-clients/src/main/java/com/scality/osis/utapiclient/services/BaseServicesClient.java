package com.scality.osis.utapiclient.services;

import com.amazonaws.*;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.AwsSyncClientParams;
import com.amazonaws.client.builder.AdvancedConfig;
import com.amazonaws.handlers.HandlerChainFactory;
import com.amazonaws.handlers.HandlerContextKey;
import com.amazonaws.http.AmazonHttpClient;
import com.amazonaws.http.DefaultErrorResponseHandler;
import com.amazonaws.http.ExecutionContext;
import com.amazonaws.http.HttpResponseHandler;
import com.amazonaws.internal.AmazonWebServiceRequestAdapter;
import com.amazonaws.metrics.RequestMetricCollector;
import com.amazonaws.protocol.json.JsonClientMetadata;
import com.amazonaws.protocol.json.JsonOperationMetadata;
import com.amazonaws.transform.StandardErrorUnmarshaller;
import com.amazonaws.transform.Unmarshaller;
import com.amazonaws.util.CredentialUtils;
import com.scality.osis.utapiclient.dto.GenericRequestMarshaller;
import com.scality.osis.utapiclient.dto.GenericRequestProtocolMarshaller;
import com.scality.osis.utapiclient.dto.GenericResponseJsonUnmarshaller;
import com.scality.osis.utapiclient.utils.UtapiClientException;
import lombok.Generated;
import org.w3c.dom.Node;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class that extends Amazon Web Service client.
 * <p>
 * Responsible for basic and common client capabilities that are the same across Utapi
 * SDK Java client (ex: setting the client endpoint, invoking the Service API).
 */
@SuppressWarnings("unchecked")
public abstract class BaseServicesClient extends AmazonWebServiceClient {

    /**
     * Constructs a new client to invoke service methods on Utapi. A credentials provider chain will be used that searches
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
     *
     * @see DefaultAWSCredentialsProviderChain
     */
    public BaseServicesClient() {
        this(configFactory.getConfig());
    }

    /**
     * Constructs a new AmazonWebServiceClient object using the specified
     * configuration.
     *
     * @param clientConfiguration The client configuration for this client.
     */
    public BaseServicesClient(ClientConfiguration clientConfiguration) {
        this(DefaultAWSCredentialsProviderChain.getInstance(), clientConfiguration);
    }

    /**
     * Constructs a new client to invoke service methods on Utapi. A credentials provider chain will be used that searches
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
     *
     * @param awsCredentials the aws credentials
     */
    public BaseServicesClient(AWSCredentials awsCredentials) {
        this(new AWSStaticCredentialsProvider(awsCredentials));
    }

    public BaseServicesClient(AWSCredentialsProvider awsCredentialsProvider) {
        this(awsCredentialsProvider, configFactory.getConfig());
    }

    public BaseServicesClient(AWSCredentials awsCredentials, ClientConfiguration clientConfiguration) {
        this(new AWSStaticCredentialsProvider(awsCredentials), clientConfiguration);
    }

    /**
     * Constructs a new client to invoke service methods on Utapi using the specified AWS account credentials provider and
     * client configuration options.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not return until the service call
     * completes.
     *
     * @param awsCredentialsProvider The AWS credentials provider which will provide credentials to authenticate requests with AWS services.
     * @param clientConfiguration    The client configuration options controlling how this client connects to Utapi (ex: proxy settings, retry
     *                               counts, etc.).
     */
    public BaseServicesClient(AWSCredentialsProvider awsCredentialsProvider, ClientConfiguration clientConfiguration) {
        this(awsCredentialsProvider, clientConfiguration, null);
    }

    public BaseServicesClient(AWSCredentialsProvider awsCredentialsProvider, ClientConfiguration clientConfiguration,
                              RequestMetricCollector requestMetricCollector) {
        super(clientConfiguration, requestMetricCollector);
        this.awsCredentialsProvider = awsCredentialsProvider;
        this.advancedConfig = AdvancedConfig.EMPTY;
        init();
    }

    /**
     * Constructs a new client to invoke service methods on Utapi using the specified parameters.
     *
     * <p>
     * All service calls made using this new client object are blocking, and will not return until the service call
     * completes.
     *
     * @param clientParams
     *        Object providing client parameters.
     */
    BaseServicesClient(AwsSyncClientParams clientParams) {
        super(clientParams);
        this.awsCredentialsProvider = clientParams.getCredentialsProvider();
        this.advancedConfig = clientParams.getAdvancedConfig();
        init();
    }

    /**
     * Constructs a new client to invoke service methods on Utapi. A credentials provider chain will be used that searches
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
     *
     * @param client *        client object.
     * @see DefaultAWSCredentialsProviderChain
     */
    public BaseServicesClient(AmazonHttpClient client) {
        this();
        this.client = client;
    }

    /**
     * Constructs a new client to invoke service methods on Utapi. A credentials provider chain will be used that searches
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
     *
     * @param client         *        client object.
     * @param awsCredentials *        the aws credentials
     */
    public BaseServicesClient(AmazonHttpClient client, AWSCredentials awsCredentials) {
        this(awsCredentials);
        this.client = client;
    }

    /**
     * Default signing name for the service.
     */
    private static final String DEFAULT_SIGNING_NAME = "s3";

    /**
     * Provider for AWS credentials.
     */
    private AWSCredentialsProvider awsCredentialsProvider;

    private AdvancedConfig advancedConfig;

    /**
     * List of exception unmarshallers for all modeled exceptions
     */
    protected final List<Unmarshaller<AmazonServiceException, Node>> exceptionUnmarshallers = new ArrayList<Unmarshaller<AmazonServiceException, Node>>();

    public AWSCredentialsProvider getAwsCredentialsProvider() {
        return awsCredentialsProvider;
    }

    @Generated
    public void setAwsCredentialsProvider(AWSCredentialsProvider awsCredentialsProvider) {
        this.awsCredentialsProvider = awsCredentialsProvider;
    }

    public AdvancedConfig getAdvancedConfig() {
        return advancedConfig;
    }

    @Generated
    public void setAdvancedConfig(AdvancedConfig advancedConfig) {
        this.advancedConfig = advancedConfig;
    }

    public List<Unmarshaller<AmazonServiceException, Node>> getExceptionUnmarshallers() {
        return exceptionUnmarshallers;
    }

    /**
     * Client configuration factory providing ClientConfigurations tailored to this client
     */
    protected static final ClientConfigurationFactory configFactory = new ClientConfigurationFactory();

    private static final com.amazonaws.protocol.json.SdkJsonProtocolFactory protocolFactory = new com.amazonaws.protocol.json.SdkJsonProtocolFactory(
            new JsonClientMetadata()
                    .withProtocolVersion("1.1")
                    .withSupportsCbor(false)
                    .withSupportsIon(false)
                    .withContentTypeOverride("")
                    .withBaseServiceExceptionClass(UtapiClientException.class));

    private void init() {
        exceptionUnmarshallers.add(new StandardErrorUnmarshaller(UtapiClientException.class));

        setServiceNameIntern(DEFAULT_SIGNING_NAME);
        HandlerChainFactory chainFactory = new HandlerChainFactory();
        requestHandler2s.addAll(chainFactory.getGlobalHandlers());
    }

    /**
     * Execute the Utapi service.
     *
     * @param <T>               the AmazonWebServiceRequest type parameter
     * @param <R>               the response DTO type parameter
     * @param requestDTO        the request dto
     * @param operationName     the operation name
     * @param requestUriPath    the request uri path
     * @param requestMarshaller the requestMarshaller
     * @param responseClassType the response class type
     * @return the AWS response wrapper
     */
    public <T extends AmazonWebServiceRequest, R> Response<R> execute(T requestDTO,
                                                                      String operationName,
                                                                      String requestUriPath,
                                                                      GenericRequestMarshaller requestMarshaller,
                                                                      Class<R> responseClassType) {

        return execute(requestDTO, operationName, requestUriPath, requestMarshaller, new GenericResponseJsonUnmarshaller<R>(responseClassType));
    }

    /**
     * Execute the Utapi service.
     *
     * @param <T>               the AmazonWebServiceRequest type parameter
     * @param <R>               the response DTO type parameter
     * @param requestDTO        the request dto
     * @param operationName     the operation name
     * @param requestUriPath    the request uri path
     * @param requestMarshaller the requestMarshaller
     * @param unmarshaller      the response unmarshaller
     * @return the AWS response wrapper
     */
    public <T extends AmazonWebServiceRequest, R> Response<R> execute(T requestDTO,
                                                                      String operationName,
                                                                      String requestUriPath,
                                                                      GenericRequestMarshaller requestMarshaller,
                                                                      Unmarshaller unmarshaller) {

        ExecutionContext executionContext = createExecutionContext(requestDTO);

        Request<T> request = new GenericRequestProtocolMarshaller<T>(protocolFactory, requestMarshaller, requestUriPath)
                .marshall(super.beforeMarshalling(requestDTO));
        request.addHandlerContext(HandlerContextKey.ENDPOINT_OVERRIDDEN, isEndpointOverridden());
        request.addHandlerContext(HandlerContextKey.SIGNING_REGION, getSigningRegion());
        request.addHandlerContext(HandlerContextKey.OPERATION_NAME, operationName);
        request.addHandlerContext(HandlerContextKey.ADVANCED_CONFIG, getAdvancedConfig());

        HttpResponseHandler<AmazonWebServiceResponse<R>> responseHandler = protocolFactory.createResponseHandler(
                new JsonOperationMetadata().withPayloadJson(true).withHasStreamingSuccessResponse(true), unmarshaller);

        return invoke(request, responseHandler, executionContext);
    }

    /**
     * Normal invoke with authentication. Credentials are required and may be overriden at the request level.
     **/
    private <X, Y extends AmazonWebServiceRequest> Response<X> invoke(Request<Y> request, HttpResponseHandler<AmazonWebServiceResponse<X>> responseHandler,
                                                                      ExecutionContext executionContext) {

        return invoke(request, responseHandler, executionContext, null, null);
    }

    /**
     * Normal invoke with authentication. Credentials are required and may be overriden at the request level.
     **/
    private <X, Y extends AmazonWebServiceRequest> Response<X> invoke(Request<Y> request, HttpResponseHandler<AmazonWebServiceResponse<X>> responseHandler,
                                                                      ExecutionContext executionContext, URI cachedEndpoint, URI uriFromEndpointTrait) {

        executionContext.setCredentialsProvider(CredentialUtils.getCredentialsProvider(request.getOriginalRequest(), getAwsCredentialsProvider()));

        return doInvoke(request, responseHandler, executionContext, cachedEndpoint, uriFromEndpointTrait);
    }

    /**
     * Invoke the request using the http client. Assumes credentials (or lack thereof) have been configured in the
     * ExecutionContext beforehand.
     **/
    private <X, Y extends AmazonWebServiceRequest> Response<X> doInvoke(Request<Y> request, HttpResponseHandler<AmazonWebServiceResponse<X>> responseHandler,
                                                                        ExecutionContext executionContext, URI discoveredEndpoint, URI uriFromEndpointTrait) {
        request.setEndpoint(endpoint);
        request.setTimeOffset(timeOffset);

        DefaultErrorResponseHandler errorResponseHandler = new DefaultErrorResponseHandler(getExceptionUnmarshallers());

        return client.execute(request, responseHandler, errorResponseHandler, executionContext, new AmazonWebServiceRequestAdapter(request.getOriginalRequest()));
    }
}
