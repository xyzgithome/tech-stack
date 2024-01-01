package com.tech.stack.spring.a04;

import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.ContextAnnotationAutowireCandidateResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

// AutowiredAnnotationBeanPostProcessor 运行分析
public class DigInAutowired {
    public static void main(String[] args) throws Throwable {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("bean2", new Bean2()); // 不会进行创建过程,依赖注入,初始化
        beanFactory.registerSingleton("bean3", new Bean3());
        beanFactory.setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver()); // 获取@Value中的值
        beanFactory.addEmbeddedValueResolver(new StandardEnvironment()::resolvePlaceholders); // ${} 的解析器

        // 1. 查找哪些属性、方法加了 @Autowired, 这称之为 InjectionMetadata
        AutowiredAnnotationBeanPostProcessor processor = new AutowiredAnnotationBeanPostProcessor();
        processor.setBeanFactory(beanFactory);

        Bean1 bean1 = new Bean1();
        System.out.println(bean1);
        // 执行依赖注入 @Autowired @Value
//         processor.postProcessProperties(null, bean1, "bean1");
        myPostProcessProperties(beanFactory, processor, bean1);
        System.out.println("---------bean1---------" + bean1);
    }

    private static void myPostProcessProperties(DefaultListableBeanFactory beanFactory,
                                                AutowiredAnnotationBeanPostProcessor processor, Bean1 bean1) throws Throwable {
        Method findAutowiringMetadata = AutowiredAnnotationBeanPostProcessor.class.getDeclaredMethod(
                "findAutowiringMetadata", String.class, Class.class, PropertyValues.class);
        findAutowiringMetadata.setAccessible(true);
        // 获取 Bean1 上加了 @Value@Autowired 的成员变量，方法参数信息
        InjectionMetadata metadata = (InjectionMetadata) findAutowiringMetadata.invoke(processor, "bean1", Bean1.class, null);
        // 调用 InjectionMetadata 来进行依赖注入, 注入时按类型查找值
//        metadata.inject(bean1, "bean1", null);
        MyInject(beanFactory, metadata, bean1);
    }

    private static void MyInject(
            DefaultListableBeanFactory beanFactory, InjectionMetadata metadata, Bean1 bean1) throws Exception {
        Field injectedElementsField = InjectionMetadata.class.getDeclaredField("injectedElements");
        injectedElementsField.setAccessible(true);
        // 获取到属性值的字节码信息
        Class<?> aClass = injectedElementsField.get(metadata).getClass();
        Method sizeMethod = aClass.getDeclaredMethod("size");
        sizeMethod.setAccessible(true);
        int size = (int) sizeMethod.invoke(injectedElementsField.get(metadata));

        for (int i = 0; i < size; i++) {
            Method getMethod = aClass.getDeclaredMethod("get", int.class);
            getMethod.setAccessible(true);
            InjectionMetadata.InjectedElement injectedElement =
                    (InjectionMetadata.InjectedElement) getMethod.invoke(injectedElementsField.get(metadata), i);

            Member injectedElementMember = injectedElement.getMember();
            Object value;
            if (injectedElementMember instanceof Field) {
                // 给成员变量 按类型查找需要依赖注入对象的值
                Field memberField = (Field) injectedElementMember;
                DependencyDescriptor dd1 = new DependencyDescriptor(memberField, true);
                value = beanFactory.doResolveDependency(
                        dd1, null, null, null);
                if (value != null) {
                    ReflectionUtils.makeAccessible(memberField);
                    memberField.set(bean1, value);
                }
            } else if (injectedElementMember instanceof Method) {
                Method memberMethod = (Method) injectedElementMember;
                DependencyDescriptor dd2 =
                        new DependencyDescriptor(new MethodParameter(memberMethod, 0), true);
                value = beanFactory.doResolveDependency(
                        dd2, null, null, null);
                if (value != null) {
                    ReflectionUtils.makeAccessible(memberMethod);
                    memberMethod.invoke(bean1, value);
                }
            }
        }
    }
}
