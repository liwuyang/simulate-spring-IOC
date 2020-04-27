package com.lwy.xml.service;

import com.lwy.xml.dao.UserDao;

/**
 * 测试手动装配，setter
 */
public class UserServiceImplProp implements UserService {

    UserDao userDaoImplFirst;

    UserDao userDaoImplSecond;

    public void find() {

        System.out.println("--------------------UserServiceImplProp");

        userDaoImplFirst.query();
        userDaoImplSecond.query();

    }

    public void setUserDaoImplFirst(UserDao userDaoImplFirst) {
        this.userDaoImplFirst = userDaoImplFirst;
    }

    public void setUserDaoImplSecond(UserDao userDaoImplSecond) {
        this.userDaoImplSecond = userDaoImplSecond;
    }
}
