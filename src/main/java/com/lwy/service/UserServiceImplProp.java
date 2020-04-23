package com.lwy.service;

import com.lwy.dao.UserDao;

/**
 * 测试手动装配，setter
 */
public class UserServiceImplProp implements UserService {

    UserDao userDaoImplFirst;

    UserDao userDaoImplSecond;

    public void find() {

        userDaoImplFirst.query();
        userDaoImplSecond.query();

        System.out.println("--------------------UserServiceImplProp");

    }

    public void setUserDaoImplFirst(UserDao userDaoImplFirst) {
        this.userDaoImplFirst = userDaoImplFirst;
    }

    public void setUserDaoImplSecond(UserDao userDaoImplSecond) {
        this.userDaoImplSecond = userDaoImplSecond;
    }
}
