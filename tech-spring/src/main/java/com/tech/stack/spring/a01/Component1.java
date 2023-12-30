package com.tech.stack.spring.a01;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class Component1 {

    private static final Logger log = LoggerFactory.getLogger(Component1.class);

    @Autowired
    private ApplicationEventPublisher context;

    public void register() {
        log.info("发布用户注册事件");
        context.publishEvent(new UserRegisteredEvent(this));
    }

}
