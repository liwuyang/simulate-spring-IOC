package com.lwy.service;

import com.lwy.dao.UserDao;

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

        userDaoImplFirst.query();
        userDaoImplSecond.query();

        System.out.println("--------------------UserServiceImplCons");

    }

}
