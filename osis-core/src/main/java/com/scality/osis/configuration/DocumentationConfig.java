/**
 * Copyright 2020 VMware, Inc.
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DocumentationConfig {

    private static final String DOC_TITLE = "Object Storage Interoperability Services API for Scality platform";
    private static final String DOC_DESCRIPTION = "This is VMware Cloud Director Object Storage Interoperability Services API for Scality platform.";
    private static final String DOC_VERSION = "2.1.0";
    public static final String PROJECT_BASE = "com.scality.osis";

    @Bean
    public OpenAPI openAPI() {

        return new OpenAPI()
                .info(new Info()
                        .title(DOC_TITLE)
                        .version(DOC_VERSION)
                        .description(DOC_DESCRIPTION)
                );
    }

}