package com.mcl.test;

import com.mcl.client.RpcProxy;
import com.mcl.service.ComServiceImpl;
import com.mcl.service.Service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:spring-zk-rpc.xml")

public class TestRPC {

    @Autowired
    private RpcProxy rpcProxy;

    @Test
    public void test() {
        ComServiceImpl comServiceImpl= rpcProxy.create(Service.class);
        String result = comServiceImpl.provideService("普通客户");
        System.out.println(result);
    }
}