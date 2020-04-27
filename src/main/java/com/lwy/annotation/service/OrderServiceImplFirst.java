package com.lwy.annotation.service;

import com.lwy.annotation.dao.OrderDao;
import com.lwy.annotation.myAnnotation.MyAutowired;
import com.lwy.annotation.myAnnotation.MyService;

@MyService
public class OrderServiceImplFirst implements OrderService {

    @MyAutowired
    OrderDao orderDao;

    public void find() {

        System.out.println("--------------------OrderServiceImplFirst");

        orderDao.query();

    }
}
