package com.scality.osis.utapiclient.utils;

import com.amazonaws.AmazonServiceException;

public class UtapiClientException extends AmazonServiceException {

    public UtapiClientException(String errorMessage, Exception cause) {
        super(errorMessage, cause);
    }

    public UtapiClientException(String errorMessage) {
        super(errorMessage);
    }
}
