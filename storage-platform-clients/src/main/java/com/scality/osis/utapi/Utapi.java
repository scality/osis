package com.scality.osis.utapi;

import com.amazonaws.services.securitytoken.model.Credentials;
import com.scality.osis.utapiclient.services.UtapiServiceClient;

public interface Utapi {

    /**
     * Returns the utapi client to invoke utapi services
     *
     * @return The utapi client object.
     */
    UtapiServiceClient getUtapiServiceClient(Credentials credentials, String region);
}
