package com.lwy.xml.service;

import com.lwy.xml.dao.UserDao;

/**
 * 测试手动装配，setter与构造方法装配同时存在
 */
public class UserServiceImplPropAndCons implements UserService {

    UserDao userDaoImplFirst;

    UserDao userDaoImplSecond;

    UserDao userDaoImplThird;

    UserDao userDaoImplFourth;

    public void find() {

        System.out.println("--------------------UserServiceImplPropAndCons");

        userDaoImplFirst.query();
        userDaoImplSecond.query();
        userDaoImplThird.query();
        userDaoImplFourth.query();

    }

    public void setUserDaoImplFirst(UserDao userDaoImplFirst) {
        this.userDaoImplFirst = userDaoImplFirst;
    }

    public void setUserDaoImplThird(UserDao userDaoImplThird) {
        this.userDaoImplThird = userDaoImplThird;
    }

    public UserServiceImplPropAndCons(UserDao userDaoImplSecond, UserDao userDaoImplFourth) {
        this.userDaoImplSecond = userDaoImplSecond;
        this.userDaoImplFourth = userDaoImplFourth;
    }
}
