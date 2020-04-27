package com.lwy.test;

import com.lwy.xml.controller.UserController;
import com.lwy.factory.XmlBeanFactory;
import com.lwy.xml.service.UserService;

public class TestXml {

    public static void main(String[] args) {

        XmlBeanFactory beanFactory = new XmlBeanFactory("spring.xml");

        UserService userServiceImplProp = (UserService) beanFactory.getBean("userServiceImplProp");

        UserService userServiceImplCons = (UserService) beanFactory.getBean("userServiceImplCons");

        UserService userServiceImplPropAndCons = (UserService) beanFactory.getBean("userServiceImplPropAndCons");

//        UserService userServiceImplByType = (UserService) beanFactory.getBean("userServiceImplByType");

        UserService userServiceImplByName = (UserService) beanFactory.getBean("userServiceImplByName");

        UserController userControllerXml = (UserController) beanFactory.getBean("userControllerXml");

        userServiceImplProp.find();
        userServiceImplCons.find();
        userServiceImplPropAndCons.find();
//        userServiceImplByType.find();
        userServiceImplByName.find();
        userControllerXml.get();

    }

}
