package org.zhaobo.ping.core.filter.loadbalance;

import org.zhaobo.ping.common.config.ServiceInstance;
import org.zhaobo.ping.core.context.GatewayContext;

/**
 * @Auther: bo
 * @Date: 2023/12/5 21:49
 * @Description: 负载均衡顶级接口
 */
public interface IGatewayLoadBalanceStrategy {

    // 通过上下文获取
    ServiceInstance choose(GatewayContext ctx);

    // 通过服务Id获取
    ServiceInstance choose(String serviceId, boolean gray);
}
