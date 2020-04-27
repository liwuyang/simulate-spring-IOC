package com.lwy.annotation.dao;

import com.lwy.annotation.myAnnotation.MyRepository;

@MyRepository(value = "fifthUserDao")
public class UserDaoImplFifth implements UserDao {
    public void query() {
        System.out.println("UserDaoImplFifth");
    }
}
