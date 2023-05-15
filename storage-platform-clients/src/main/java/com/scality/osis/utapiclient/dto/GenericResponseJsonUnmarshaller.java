package com.scality.osis.utapiclient.dto;

import com.amazonaws.transform.JsonUnmarshallerContext;
import com.amazonaws.transform.Unmarshaller;
import com.amazonaws.util.IOUtils;
import com.google.gson.Gson;
import com.scality.osis.utapiclient.utils.UtapiClientException;

/**
 * A GenericResponseJsonUnmarshaller that implements JsonUnmarshaller to unmarshall response object from JSON String
 * extracted from JsonUnmarshallerContext
 */
public class GenericResponseJsonUnmarshaller<T> implements Unmarshaller<T, JsonUnmarshallerContext> {
    final Class<T> typeParameterClass;

    public GenericResponseJsonUnmarshaller(Class<T> typeParameterClass) {
        this.typeParameterClass = typeParameterClass;
    }

    /**
     * unmarshall response object from JSON String extracted from JsonUnmarshallerContext
     *
     * @param context the JsonUnmarshallerContext with JSON response string
     * @return result unmarshalled response object
     */
    @Override
    public T unmarshall(JsonUnmarshallerContext context) {
        T result = null;
        try {
            if (context.getHttpResponse().getContent() != null) {
                String response = IOUtils.toString(context.getHttpResponse().getContent());
                result = new Gson().fromJson(response, typeParameterClass);
            }
        } catch (Exception e) {
            throw new UtapiClientException("Unable to unmarshall response content", e);
        }
        return result;
    }
}
