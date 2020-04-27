package com.lwy.xml.service;

import com.lwy.xml.dao.UserDao;

/**
 * 测试自动装配，byType
 */
public class UserServiceImplByType implements UserService {

    UserDao userDaoImplFirst;

    public void find() {

        System.out.println("--------------------UserServiceImplByType");

        userDaoImplFirst.query();

    }

    public void setUserDaoImplFirst(UserDao userDaoImplFirst) {
        this.userDaoImplFirst = userDaoImplFirst;
    }
}
