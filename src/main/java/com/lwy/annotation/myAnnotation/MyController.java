package com.lwy.annotation.myAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 自定义Controller注解
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface MyController {

    public String value() default "";

}
