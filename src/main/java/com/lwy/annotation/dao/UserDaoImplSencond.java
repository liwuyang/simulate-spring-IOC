package com.lwy.annotation.dao;

import com.lwy.annotation.myAnnotation.MyRepository;

@MyRepository
public class UserDaoImplSencond implements UserDao {

    public void query() {

        System.out.println("UserDaoImplSencond");

    }

}
