package com.lwy.xml.controller;

import com.lwy.xml.service.UserService;

/**
 * 测试xml多层注入
 */
public class UserControllerXml implements UserController {

    UserService userServiceImplByName;

//    UserService userServiceImplByType;

    UserService userServiceImplCons;

    UserService userServiceImplProp;

    UserService userServiceImplPropAndCons;

    public void get() {

        System.out.println("--------------------UserControllerXml");

        userServiceImplByName.find();
//        userServiceImplByType.find();
        userServiceImplCons.find();
        userServiceImplProp.find();
        userServiceImplPropAndCons.find();

    }
}
