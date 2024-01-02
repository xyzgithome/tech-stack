package com.tech.stack.spring.a05;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;

import java.io.IOException;
import java.util.Set;

/**
 * 模拟实现ConfigurationClassPostProcessor bean工厂后置处理器
 * 解析@Bean注解标注的bean
 */
public class AtBeanPostProcessor implements BeanDefinitionRegistryPostProcessor {
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {

    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanFactory) throws BeansException {
        try {
            CachingMetadataReaderFactory factory = new CachingMetadataReaderFactory();
            MetadataReader reader = factory
                    .getMetadataReader(new ClassPathResource("com/tech/stack/spring/a05/Config.class"));
            Set<MethodMetadata> methods = reader.getAnnotationMetadata().getAnnotatedMethods(Bean.class.getName());

            // 将找到的methods构建成bean定义
            for (MethodMetadata method : methods) {
                BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition();
                // 给bean定义设置工厂方法
                builder.setFactoryMethodOnBean(method.getMethodName(), "config");

                // 对于构造方法或者工厂方法创建spring实例的
                // 参数(sqlSessionFactoryBean(DataSource dataSource))选择AUTOWIRE_CONSTRUCTOR
                builder.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);

                // 获取方法@Bean注解的initMethod属性值: init
                String initMethod = method.getAnnotationAttributes(Bean.class.getName()).get("initMethod").toString();
                if (initMethod.length() > 0) {
                    builder.setInitMethodName(initMethod);
                }

                beanFactory.registerBeanDefinition(method.getMethodName(), builder.getBeanDefinition());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
