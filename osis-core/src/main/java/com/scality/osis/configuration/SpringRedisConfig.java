/**
 *Copyright 2021 Scality, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashSet;

@Configuration
public class SpringRedisConfig {
    @Autowired
    private RedisProperties redisProperties;

    @Bean
    protected LettuceConnectionFactory redisConnectionFactory() {
        RedisSentinelConfiguration sentinelConfig
                = new RedisSentinelConfiguration(
                            redisProperties.getSentinel().getMaster(),
                            new HashSet<>(redisProperties.getSentinel().getNodes()));

        sentinelConfig.setSentinelPassword(RedisPassword.of(redisProperties.getPassword()));
        sentinelConfig.setPassword(RedisPassword.of(redisProperties.getPassword()));

        LettuceClientConfiguration lettuceClientConfig = redisProperties.isSsl() ?
                                            LettuceClientConfiguration.builder().useSsl().build() :
                                            LettuceClientConfiguration.defaultConfiguration();
        return new LettuceConnectionFactory(sentinelConfig, lettuceClientConfig);
    }

    @Bean
    public StringRedisTemplate redisTemplate() {
        StringRedisTemplate redisTemplate = new StringRedisTemplate(redisConnectionFactory());
        return redisTemplate;
    }
}
