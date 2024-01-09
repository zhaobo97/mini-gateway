package org.zhaobo.ping.core.filter;

import java.lang.annotation.*;

/**
 * @Auther: bo
 * @Date: 2023/12/5 9:28
 * @Description: 过滤器注解类
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FilterInfo {

    // 过滤器id
    String id();

    // 过滤器名称
    String name() default "";

    // 过滤器排序
    int order() default 0;
}
