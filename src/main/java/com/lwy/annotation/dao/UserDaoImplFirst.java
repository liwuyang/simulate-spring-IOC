package com.lwy.annotation.dao;

import com.lwy.annotation.myAnnotation.MyRepository;

@MyRepository
public class UserDaoImplFirst implements UserDao {

    public void query() {

        System.out.println("UserDaoImplFirst");

    }
}
