/**
 *Copyright 2020 VMware, Inc.
 *Copyright 2021 Scality, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * @author ges
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class
})
@EnableAsync
public class Application {
    public static void main(String[] args) throws Exception {
        new SpringApplication(Application.class).run(args);
    }
}