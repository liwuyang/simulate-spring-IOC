package com.lwy.annotation.dao;

import com.lwy.annotation.myAnnotation.MyRepository;

@MyRepository
public class OrderDaoImplFirst implements OrderDao {

    public void query() {

        System.out.println("OrderDaoImplFirst");

    }

}
