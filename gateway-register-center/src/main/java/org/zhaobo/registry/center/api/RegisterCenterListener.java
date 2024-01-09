package org.zhaobo.registry.center.api;

import org.zhaobo.common.config.ServiceDefinition;
import org.zhaobo.common.config.ServiceInstance;

import java.util.Set;

/**
 * @Auther: bo
 * @Date: 2023/12/3 12:49
 * @Description:
 */

public interface RegisterCenterListener {

    void onChange(ServiceDefinition serviceDefinition, Set<ServiceInstance> serviceInstanceSet);
}
