package org.zhaobo.client.core;

import java.lang.annotation.*;

/**
 * 必须在服务的方法上强制声明！！
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiInvoker {
    String path();
}
