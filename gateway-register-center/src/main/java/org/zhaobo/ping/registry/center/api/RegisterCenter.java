package org.zhaobo.ping.registry.center.api;

import org.zhaobo.ping.common.config.ServiceDefinition;
import org.zhaobo.ping.common.config.ServiceInstance;

/**
 * @Auther: bo
 * @Date: 2023/12/3 12:34
 * @Description:
 */
public interface RegisterCenter {

    /**
     * 初始化
     * @param registerAddress
     * @param namespace
     * @param group
     */
    void init(String registerAddress,String namespace, String group, String username, String password);

    /**
     * 注册
     * @param serviceDefinition
     * @param serviceInstance
     */
    void register(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance);

    /**
     * 注销
     * @param serviceDefinition
     * @param serviceInstance
     */
    void deregister(ServiceDefinition serviceDefinition, ServiceInstance serviceInstance);

    /**
     * 订阅所有服务变更
     * @param listener
     */
    void subscribeAllServices(RegisterCenterListener listener);


}
