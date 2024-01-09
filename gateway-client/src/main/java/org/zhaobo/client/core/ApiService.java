package org.zhaobo.client.core;

import java.lang.annotation.*;

/**
 * 服务定义，注册到nacos的服务
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiService {
    String serviceId();

    String version() default "1.0.0";

    ApiProtocol protocol();

    String patternPath();
}
