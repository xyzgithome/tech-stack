package com.tech.stack.spring.a01;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class Component2 {

    private static final Logger log = LoggerFactory.getLogger(Component2.class);

    // 还可以通过实现ApplicationListener<UserRegisteredEvent>接口进行事件监听
    @EventListener
    public void aaa(UserRegisteredEvent event) {
        log.info("监听用户注册事件");
    }
}
