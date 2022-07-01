/**
 *Copyright 2020 VMware, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author ges
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface NotImplement {
    String name() default "";
}
