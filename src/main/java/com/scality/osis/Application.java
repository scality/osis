/**
 *Copyright 2020 VMware, Inc.
 *Copyright 2021 Scality, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * @author ges
 */
@ComponentScan(basePackages = {"com.scality.osis"})
@SpringBootApplication
@EnableSwagger2
@EnableAsync
public class Application {
    public static void main(String[] args) throws Exception {
        new SpringApplication(Application.class).run(args);
    }
}
