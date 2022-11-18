/**
 * Copyright 2020 VMware, Inc.
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.resource;

import com.scality.osis.annotation.NotImplement;
import com.scality.osis.configuration.DocumentationConfig;
import com.scality.osis.model.ScalityOsisCaps;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ScalityOsisCapsManager implements InitializingBean {

    private ApplicationContext applicationContext;
    private final ScalityOsisCaps osisCaps;

    public ScalityOsisCapsManager(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.osisCaps = new ScalityOsisCaps();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Set<Class> classes = getAnnotatedClasses(RestController.class);
        for (Class clazz : classes) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(NotImplement.class)) {
                    NotImplement a = method.getAnnotation(NotImplement.class);
                    if (this.osisCaps.getOptionalApis().containsKey(a.name())) {
                        this.osisCaps.getOptionalApis().put(a.name(), false);
                    }
                }
            }
        }
    }

    public List<String> getNotImplements() {
        return this.osisCaps.getOptionalApis().entrySet().stream()
                .filter(entry -> entry.getValue().equals(Boolean.FALSE))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private Set<Class> getAnnotatedClasses(Class<? extends Annotation>... annotationTypes) {

        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        Arrays.stream(annotationTypes).forEach(at -> {
            provider.addIncludeFilter(new AnnotationTypeFilter(at));
        });
        return provider.findCandidateComponents(DocumentationConfig.PROJECT_BASE).stream()
                .map(bean -> {
                    try {
                        return Class.forName(bean.getBeanClassName());
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toSet());
    }
}
