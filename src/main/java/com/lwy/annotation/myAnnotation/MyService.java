package com.lwy.annotation.myAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 自定义Service注解
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface MyService {

    public String value() default "";

}
