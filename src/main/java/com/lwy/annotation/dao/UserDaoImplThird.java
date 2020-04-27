package com.lwy.annotation.dao;

import com.lwy.annotation.myAnnotation.MyRepository;

@MyRepository
public class UserDaoImplThird implements UserDao {

    public void query() {

        System.out.println("UserDaoImplThird");

    }
    
}
