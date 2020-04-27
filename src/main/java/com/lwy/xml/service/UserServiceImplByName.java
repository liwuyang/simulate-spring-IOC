package com.lwy.xml.service;

import com.lwy.xml.dao.UserDao;

/**
 * 测试自动装配，byName
 */
public class UserServiceImplByName implements UserService {

    UserDao userDaoImplFirst;

    UserDao userDaoImplSecond;

    public void find() {

        System.out.println("--------------------UserServiceImplByName");

        userDaoImplFirst.query();
        userDaoImplSecond.query();

    }
}
