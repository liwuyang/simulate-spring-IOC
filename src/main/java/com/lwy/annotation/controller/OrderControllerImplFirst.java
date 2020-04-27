package com.lwy.annotation.controller;

import com.lwy.annotation.myAnnotation.MyAutowired;
import com.lwy.annotation.myAnnotation.MyController;
import com.lwy.annotation.service.OrderService;

@MyController(value = "orderControllerFirst")
public class OrderControllerImplFirst implements OrderController {

    @MyAutowired
    OrderService orderService;

    public void get() {

        System.out.println("--------------------OrderControllerImplFirst");

        orderService.find();

    }
}
