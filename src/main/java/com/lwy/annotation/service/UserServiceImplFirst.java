package com.lwy.annotation.service;

import com.lwy.annotation.dao.UserDao;
import com.lwy.annotation.myAnnotation.MyAutowired;
import com.lwy.annotation.myAnnotation.MyService;

@MyService
public class UserServiceImplFirst implements UserService {

    @MyAutowired
    UserDao userDaoImplFirst;

    @MyAutowired
    UserDao userDaoImplSencond;

    @MyAutowired
    UserDao userDaoImplThird;

    @MyAutowired
    UserDao userDaoImplFourth;

    @MyAutowired
    UserDao fifthUserDao;

    public void find() {

        System.out.println("--------------------UserServiceImplFirst");

        userDaoImplFirst.query();
        userDaoImplSencond.query();
        userDaoImplThird.query();
        userDaoImplFourth.query();
        fifthUserDao.query();

    }
}
