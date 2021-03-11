/**
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.vaultadmin;

import com.google.common.base.Strings;
import com.scality.vaultadmin.impl.VaultAdminImpl;

import java.util.Arrays;

/**
 * Fluent builder for {@link VaultAdmin}. Use of the builder is preferred over using constructors of
 * the client class.
 *
 * <p>Created by saitharun on 2021/1/10.
 */
public class VaultAdminBuilder {
  private String accessKey;
  private String secretKey;
  private String endpoint;

  /**
   * Sets the access key to be used by the client.
   *
   * <p>Note that the corresponding user, i.g, the administrator, should has proper administrative
   * capabilities. See more about administrative capabilities <a
   * href="http://docs.scality.com/docs/master/radosgw/admin/#add-remove-admin-capabilities">here</a>
   *
   * @param accessKey Access key to use.
   * @return This object for method chaining.
   */
  public VaultAdminBuilder setAccessKey(String accessKey) {
    this.accessKey = accessKey;
    return this;
  }

  /**
   * Sets the secret key to be used by the client.
   *
   * <p>Note that the corresponding user, i.g, the administrator, should has proper administrative
   * capabilities. See more about administrative capabilities <a
   * href="http://docs.scality.com/docs/master/radosgw/admin/#add-remove-admin-capabilities">here</a>
   *
   * @param secretKey Secret key to use.
   * @return This object for method chaining.
   */
  public VaultAdminBuilder setSecretKey(String secretKey) {
    this.secretKey = secretKey;
    return this;
  }

  /**
   * Sets the endpoint to be used for requests.
   *
   * <p>For example: http://127.0.0.1:80/admin
   *
   * <p>Note that the admin API in the corresponding Radosgw should be enabled.
   *
   * @param endpoint Endpoint to use.
   * @return This object for method chaining.
   */
  public VaultAdminBuilder setEndpoint(String endpoint) {
    this.endpoint = endpoint;
    return this;
  }

  /**
   * Builds a client with the configure properties.
   *
   * @return Client instance to make API calls with.
   */
  public VaultAdmin build() {
    if (Arrays.asList(accessKey, secretKey, endpoint).stream().anyMatch(Strings::isNullOrEmpty)) {
      throw new IllegalArgumentException("Missing required parameter to build the instance.");
    }
    return new VaultAdminImpl(accessKey, secretKey, endpoint);
  }
}
