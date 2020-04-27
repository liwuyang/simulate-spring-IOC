package com.lwy.xml.service;

import com.lwy.xml.dao.UserDao;

/**
 * 测试手动装配，构造方法
 */
public class UserServiceImplCons implements UserService {

    UserDao userDaoImplFirst;

    UserDao userDaoImplSecond;

    public UserServiceImplCons(UserDao userDaoImplFirst, UserDao userDaoImplSecond) {
        this.userDaoImplFirst = userDaoImplFirst;
        this.userDaoImplSecond = userDaoImplSecond;
    }

    public void find() {

        System.out.println("--------------------UserServiceImplCons");

        userDaoImplFirst.query();
        userDaoImplSecond.query();

    }

}
