package org.zhaobo.ping.registry.center.api;

import org.zhaobo.ping.common.config.ServiceDefinition;
import org.zhaobo.ping.common.config.ServiceInstance;

import java.util.Set;

/**
 * @Auther: bo
 * @Date: 2023/12/3 12:49
 * @Description:
 */

public interface RegisterCenterListener {

    void onChange(ServiceDefinition serviceDefinition, Set<ServiceInstance> serviceInstanceSet);
}
