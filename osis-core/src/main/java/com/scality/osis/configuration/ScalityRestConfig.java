/**
 *Copyright 2020 VMware, Inc.
 *Copyright 2021 Scality, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.configuration;

import com.scality.osis.ScalityAppEnv;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import static com.scality.osis.utils.ScalityConstants.ASYNC_THREADPOOL_NAME_PREFIX;

@Configuration
public class ScalityRestConfig {
    @Autowired
    private ScalityAppEnv env;

    @Bean
    public ClientHttpRequestFactory simpleClientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setReadTimeout(10000);
        factory.setConnectTimeout(10000);
        return factory;
    }

    @Bean
    public Executor asyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(env.getAsyncExecutorCorePoolSize());
        executor.setMaxPoolSize(env.getAsyncExecutorMaxPoolSize());
        executor.setQueueCapacity(env.getAsyncExecutorQueueCapacity());
        executor.setThreadNamePrefix(ASYNC_THREADPOOL_NAME_PREFIX);
        executor.initialize();
        return executor;
    }
}
