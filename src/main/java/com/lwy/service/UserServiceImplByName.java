package com.lwy.service;

import com.lwy.dao.UserDao;

/**
 * 测试自动装配，byName
 */
public class UserServiceImplByName implements UserService {

    UserDao userDaoImplFirst;

    UserDao userDaoImplSecond;

    public void find() {

        userDaoImplFirst.query();
        userDaoImplSecond.query();

        System.out.println("--------------------UserServiceImplByName");

    }
}
