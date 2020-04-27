package com.lwy.annotation.myAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 自定义Repository注解
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface MyRepository {

    public String value() default "";

}
