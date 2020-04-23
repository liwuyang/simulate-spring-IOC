package com.lwy.service;

import com.lwy.dao.UserDao;

/**
 * 测试自动装配，byType
 */
public class UserServiceImplByType implements UserService {

    UserDao userDaoImplFirst;

    public void find() {

        userDaoImplFirst.query();

        System.out.println("--------------------UserServiceImplByType");

    }

    public void setUserDaoImplFirst(UserDao userDaoImplFirst) {
        this.userDaoImplFirst = userDaoImplFirst;
    }
}
