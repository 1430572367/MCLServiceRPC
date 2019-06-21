package com.mcl.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:spring-zk-rpc-server.xml")
public class ServerTest {
    @Test
    public void serverTest() {
        new ClassPathXmlApplicationContext("spring-zk-rpc-server.xml");
    }
}