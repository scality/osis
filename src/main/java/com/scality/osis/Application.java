/**
 *Copyright 2020 VMware, Inc.
 *Copyright 2021 Scality, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * @author ges
 */
@ComponentScan(basePackages = {"com.vmware.osis","com.scality.osis"})
@SpringBootApplication
@EnableSwagger2
public class Application {
    public static void main(String[] args) throws Exception {
        new SpringApplication(Application.class).run(args);
    }
}
