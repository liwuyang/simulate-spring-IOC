package com.lwy.test;

import com.lwy.factory.BeanFactory;
import com.lwy.service.UserService;

public class Test {

    public static void main(String[] args) {

        BeanFactory beanFactory = new BeanFactory("spring.xml");

        UserService userServiceImplProp = (UserService) beanFactory.getBean("userServiceImplProp");

        UserService userServiceImplCons = (UserService) beanFactory.getBean("userServiceImplCons");

        UserService userServiceImplPropAndCons = (UserService) beanFactory.getBean("userServiceImplPropAndCons");

//        UserService userServiceImplByType = (UserService) beanFactory.getBean("userServiceImplByType");

        UserService userServiceImplByName = (UserService) beanFactory.getBean("userServiceImplByName");

        userServiceImplProp.find();
        userServiceImplCons.find();
        userServiceImplPropAndCons.find();
//        userServiceImplByType.find();
        userServiceImplByName.find();

    }

}
