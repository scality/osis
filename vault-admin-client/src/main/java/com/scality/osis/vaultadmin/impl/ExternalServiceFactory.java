/**
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.vaultadmin.impl;

import com.amazonaws.Response;
import com.scality.osis.vaultadmin.utils.ErrorUtils;
import com.scality.vaultclient.services.VaultClientException;

import java.util.function.Function;

/**
 * A Generic Admin class with generic methods for all external service calls
 */
public final class ExternalServiceFactory {
    /**
     * Execute vault service.
     *
     * @param <T>      the Input dto type parameter
     * @param <R>      the Output dto type parameter
     * @param vaultMethod the vaultMethod that calls Vault service with T type input and Response<R> type output
     * @param request       the request input dto
     * @return the r response dto
     */
    public static <T,R> R executeVaultService(Function<T, Response<R>> vaultMethod, T request) {
        try {
            com.amazonaws.Response<R> response = vaultMethod.apply(request);
            if (null!= response.getHttpResponse() && ErrorUtils.isSuccessful(response.getHttpResponse().getStatusCode())) {
                return response.getAwsResponse();
            } else {
                throw ErrorUtils.parseError(response.getHttpResponse());
            }
        } catch (VaultServiceException e) {
            throw e;
        }catch (VaultClientException e){
            throw ErrorUtils.parseError(e);
        } catch (Exception e) {
            throw new VaultServiceException(500, "Exception", e);
        }
    }
}
