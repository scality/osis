/**
 * Copyright 2020 VMware, Inc.
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.configuration;

import com.scality.osis.App;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.SimpleDateFormat;
import java.util.Date;

@Configuration
public class DocumentationConfig {

    private static final String DOC_TITLE = "Object Storage Interoperability Services API for Scality platform";
    private static final String DOC_DESCRIPTION = "This is VMware Cloud Director Object Storage Interoperability Services API for Scality platform.";
    public static final String PROJECT_BASE = "com.scality.osis";

    @Bean
    public OpenAPI openAPI() {

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
        String buildDate = formatter.format(new Date(App.DATE));

        return new OpenAPI()
                .info(new Info()
                        .title(DOC_TITLE)
                        .version(App.VERSION)
                        .description(DOC_DESCRIPTION + "<br/>Build date : " + buildDate)
                );
    }

}
