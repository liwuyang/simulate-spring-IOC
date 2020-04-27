package com.lwy.annotation.controller;

import com.lwy.annotation.myAnnotation.MyAutowired;
import com.lwy.annotation.myAnnotation.MyController;
import com.lwy.annotation.service.UserService;

@MyController
public class UserControllerImplFirst implements UserController {

    @MyAutowired
    UserService userServiceImplFirst;

    public void get() {

        System.out.println("--------------------UserControllerImplFirst");

        userServiceImplFirst.find();

    }
}
