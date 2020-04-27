package com.lwy.test;

import com.lwy.annotation.controller.UserController;
import com.lwy.factory.AnnotationBeanFactory;

public class TestAnnotation {

    public static void main(String[] args) {

        AnnotationBeanFactory annotationBeanFactory = new AnnotationBeanFactory("com.lwy.annotation");

        UserController userController = (UserController) annotationBeanFactory.getBean("userControllerImplFirst");

        userController.get();

    }

}
