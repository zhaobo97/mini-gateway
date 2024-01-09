package org.zhaobo.ping.core.filter.loadbalance;

import lombok.extern.slf4j.Slf4j;
import org.zhaobo.ping.common.config.DynamicConfigManager;
import org.zhaobo.ping.common.config.ServiceInstance;
import org.zhaobo.ping.common.enums.ResponseCode;
import org.zhaobo.ping.common.exception.NotFoundException;
import org.zhaobo.ping.core.context.GatewayContext;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Auther: bo
 * @Date: 2023/12/5 22:07
 * @Description:
 */
@Slf4j
public class RoundRobinLoadBalanceStrategy implements IGatewayLoadBalanceStrategy {

    private final AtomicInteger position = new AtomicInteger(1);

    private final String serviceId;

    // 为什么用map保存起来？因为每个服务都有各自的position，每个轮询策略对象都应该是单例
    private static final Map<String, RoundRobinLoadBalanceStrategy> map = new ConcurrentHashMap<>();

    public static RoundRobinLoadBalanceStrategy getInstance(String serviceId) {
        RoundRobinLoadBalanceStrategy roundRobinLoadBalanceRule = map.get(serviceId);
        if (roundRobinLoadBalanceRule == null) {
            roundRobinLoadBalanceRule = new RoundRobinLoadBalanceStrategy(serviceId);
            map.put(serviceId, roundRobinLoadBalanceRule);
        }
        return roundRobinLoadBalanceRule;
    }

    public RoundRobinLoadBalanceStrategy(String serviceId) {
        this.serviceId = serviceId;
    }

    @Override
    public ServiceInstance choose(GatewayContext ctx) {
        return choose(ctx.getUniqueId(), ctx.isGray());
    }

    @Override
    public ServiceInstance choose(String serviceId, boolean gray) {
        Set<ServiceInstance> serviceInstanceSet = DynamicConfigManager.getInstance()
                .getServiceInstanceByUniqueId(serviceId, gray);
        if (serviceInstanceSet.isEmpty()) {
            log.warn("No instance available for: {}", serviceId);
            throw new NotFoundException(ResponseCode.SERVICE_INSTANCE_NOT_FOUND);
        }
        ArrayList<ServiceInstance> serviceInstances = new ArrayList<>(serviceInstanceSet);
        if (serviceInstances.isEmpty()) {
            log.warn("No instance available for: {}", serviceId);
            return null;
        } else {
            int pos = Math.abs(this.position.incrementAndGet());
            int size = serviceInstances.size();
            return serviceInstances.get(pos % size);
        }
    }
}
