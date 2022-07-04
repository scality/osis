/**
 * Copyright 2020 VMware, Inc.
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.resource;

import com.scality.osis.model.ScalityOsisCaps;
import com.scality.osis.annotation.NotImplement;
import com.vmware.osis.resource.OsisCapsManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class ScalityOsisCapsManager extends OsisCapsManager {

    @Autowired
    private ApplicationContext applicationContext;

    private ScalityOsisCaps osisCaps = new ScalityOsisCaps();

    @Override
    public void afterPropertiesSet() throws Exception {
        Map<String, Object> rcBeans = getBeans(RestController.class);
        for (Object bean : rcBeans.values()) {
            for (Method method : bean.getClass().getDeclaredMethods()) {
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
        return this.osisCaps.getOptionalApis().entrySet().stream().filter(not(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private Map<String, Object> getBeans(Class<? extends Annotation>... annotationTypes) {
        Map<String, Object> result = new LinkedHashMap<>();
        Arrays.stream(annotationTypes).forEach(at -> result.putAll(applicationContext.getBeansWithAnnotation(at)));
        return result;
    }

    private <T> Predicate<T> not(Predicate<T> t) {
        return t.negate();
    }
}
