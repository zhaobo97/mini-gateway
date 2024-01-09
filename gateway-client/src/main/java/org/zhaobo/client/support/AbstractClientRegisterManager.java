package org.zhaobo.client.support;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.zhaobo.client.core.ApiProperties;
import org.zhaobo.ping.common.config.ServiceDefinition;
import org.zhaobo.ping.common.config.ServiceInstance;
import org.zhaobo.ping.registry.center.api.RegisterCenter;

import java.util.ServiceLoader;

/**
 * @Auther: bo
 * @Date: 2023/12/4 16:17
 * @Description:
 */
@Slf4j
public abstract class AbstractClientRegisterManager {
    @Getter
    private ApiProperties apiProperties;

    private RegisterCenter registerCenter;

    protected AbstractClientRegisterManager(ApiProperties apiProperties){
        this.apiProperties = apiProperties;

        // 初始化注册中心对象,java SPI
        ServiceLoader<RegisterCenter> serviceLoader = ServiceLoader.load(RegisterCenter.class);
        registerCenter = serviceLoader.findFirst().orElseThrow(()->{
            log.error("not found RegisterCenter impl");
            return new RuntimeException("not found RegisterCenter impl");
        });
        // 所有的注册中心provider都要实现init
        registerCenter.init(apiProperties.getRegisterAddress(), apiProperties.getNamespace(), apiProperties.getGroup(),
                apiProperties.getUsername(), apiProperties.getPassword());
    }

    protected void register(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance){
        registerCenter.register(serviceDefinition, serviceInstance);
    }


}
