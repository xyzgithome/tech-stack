package com.tech.stack.spring.a02;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import javax.annotation.Resource;
import java.util.Objects;

public class TestBeanFactory {

    public static void main(String[] args) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        // bean 的定义（class类信息, scope(单例、原型), 初始化方法, 销毁方法）
        AbstractBeanDefinition beanDefinition =
                BeanDefinitionBuilder.genericBeanDefinition(Config.class).setScope("singleton").getBeanDefinition();
        beanFactory.registerBeanDefinition("config", beanDefinition);
        // 此时bean工厂中的bean定义只有config
        for (String beanDefinitionName : beanFactory.getBeanDefinitionNames()) {
            System.out.println(beanDefinitionName);
        }
        System.out.println("--------------------------------------------------------------------------------------");

        // 向容器添加一组基础设施的PostProcessor的bean定义
        AnnotationConfigUtils.registerAnnotationConfigProcessors(beanFactory);
        for (String beanDefinitionName : beanFactory.getBeanDefinitionNames()) {
            System.out.println(beanDefinitionName);
        }
        System.out.println("--------------------------------------------------------------------------------------");


        // 执行beanFactory的后置处理器：BeanFactory后处理器主要功能，补充了一些bean定义 bean1 bean2
        // org.springframework.context.annotation.internalConfigurationAnnotationProcessor -> {ConfigurationClassPostProcessor@1098}
        // org.springframework.context.event.internalEventListenerProcessor -> {EventListenerMethodProcessor@1271}
        beanFactory.getBeansOfType(BeanFactoryPostProcessor.class).values().forEach(beanFactoryPostProcessor -> {
            beanFactoryPostProcessor.postProcessBeanFactory(beanFactory);
        });
        for (String beanDefinitionName : beanFactory.getBeanDefinitionNames()) {
            System.out.println(beanDefinitionName);
        }
        System.out.println("--------------------------------------------------------------------------------------");

        // 输出null，因为没有执行bean的后置处理器
        // 用于解析@Autowired注解：org.springframework.context.annotation.internalAutowiredAnnotationProcessor -> {AutowiredAnnotationBeanPostProcessor@1810}
        // 用于解析@Resource注解：org.springframework.context.annotation.internalCommonAnnotationProcessor -> {CommonAnnotationBeanPostProcessor@1812}
//        System.out.println(beanFactory.getBean("bean1", Bean1.class).getBean2());
//        System.out.println("--------------------------------------------------------------------------------------");

        // 往bean工厂添加bean后置处理器, 使得bean工厂在创建bean实例时知道使用哪些bean的后置处理器
        beanFactory.getBeansOfType(BeanPostProcessor.class).values().forEach(beanFactory::addBeanPostProcessor);
//        System.out.println(beanFactory.getBean("bean1", Bean1.class).getBean2());
//        System.out.println("--------------------------------------------------------------------------------------");

        // bean工厂默认都是延迟创建bean实例，在调用以下方法使得提前创建好所有单例bean
        beanFactory.preInstantiateSingletons();
        System.out.println("--------------------------------------------------------------------------------------");
        System.out.println(beanFactory.getBean(Bean1.class).getBean2());
        System.out.println(beanFactory.getBean(Bean1.class).getInter());
        /*
            学到了什么:
            a. beanFactory 不会做的事
                   1. 不会主动调用 BeanFactory 后处理器
                   2. 不会主动添加 Bean 后处理器
                   3. 不会主动初始化单例
                   4. 不会解析 ${ } 与 #{ }
            b. bean后处理器会有排序的逻辑
         */
    }

    @Configuration
    static class Config {
        @Bean
        public Bean1 bean1() {
            return new Bean1();
        }

        @Bean
        public Bean2 bean2() {
            return new Bean2();
        }

        @Bean
        public Bean3 bean3() {
            return new Bean3();
        }

        @Bean
        public Bean4 bean4() {
            return new Bean4();
        }
    }

    interface Inter {}

    static class Bean3 implements Inter {}

    static class Bean4 implements Inter {}

    static class Bean1 {
        private static final Logger log = LoggerFactory.getLogger(Bean1.class);

        public Bean1() {
            log.debug("构造 Bean1()");
        }

        @Autowired
        private Bean2 bean2;

        public Bean2 getBean2() {
            return bean2;
        }

        @Autowired
        @Resource(name = "bean4")
        private Inter bean3;

        public Inter getInter() {
            return bean3;
        }
    }

    static class Bean2 {
        private static final Logger log = LoggerFactory.getLogger(Bean2.class);

        public Bean2() {
            log.debug("构造 Bean2()");
        }
    }

    /**
     * @Autowired注解
     * 会优先根据类型匹配，类型匹配了多个会根据成员变量的名称去匹配，字段名匹配不上则会抛出异常NoUniqueBeanDefinitionException
     *
     * @Resource注解
     * 会优先根据成员变量的名称去匹配
     *
     * @Autowired
     * @Resource(name = "bean4")
     * private Inter bean3;
     *
     * 以上最终注入的是@Autowired实例，原因是因为spring的后置处理器会有排序机制
     * CommonAnnotationProcessor和AutowiredAnnotationProcessor都实现了PriorityOrdered接口
     * 通过setOrder方法设置了各自的优先级大小，
     * "Autowired:" + (Ordered.LOWEST_PRECEDENCE - 2)
     * "Common:" + (Ordered.LOWEST_PRECEDENCE - 3)
     * 所以spring容器会优先使用@Autowired注解进行bean的注入
     */
}
