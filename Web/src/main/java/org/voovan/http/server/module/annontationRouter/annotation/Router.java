package org.voovan.http.server.module.annontationRouter.annotation;

import org.voovan.http.server.HttpContentType;

import java.lang.annotation.*;

/**
 * 路由注解类
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
@Repeatable(value = Routers.class)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Router {
    /**
     * 请求的 URL
     * @return 请求路径
     */
    String path() default "";

    /**
     *
     * @return 默认路径
     */
    String value() default "";

    /**
     * 请求的方法
     * @return 请求的方法
     */
    String[] method() default "";


    /**
     * Content-Type 配置
     * @return
     */
    HttpContentType contentType() default HttpContentType.TEXT;

    /**
     * 定义路由类是否采用单例模式
     * 在类上则标识类会被提前实例化, 在路由方法上,则使用提前实例化的类进行调用
     * 在方法上无效
     * @return true: 单例模式, false: 非单例模式
     */
    boolean singleton() default true;
}
