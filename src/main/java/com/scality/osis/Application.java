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
 * @author VMware, Inc.
 * @author Scality, Inc.
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class
})
@EnableAsync
public class Application {
    /**
     * The main entry point for the application. This method is called when the program is run
     * and is responsible for starting the Spring Boot application.
     * @param args array of command-line arguments
     * @throws Exception If an error occurs while initializing the Spring Boot application
     */
    public static void main(String[] args) throws Exception {
        new SpringApplication(Application.class).run(args);
    }
}
