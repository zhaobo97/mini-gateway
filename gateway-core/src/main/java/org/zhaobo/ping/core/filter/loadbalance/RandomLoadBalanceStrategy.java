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
import java.util.concurrent.ThreadLocalRandom;

/**
 * @Auther: bo
 * @Date: 2023/12/5 21:51
 * @Description: 随机负载均衡
 */
@Slf4j
public class RandomLoadBalanceStrategy implements IGatewayLoadBalanceStrategy{

    private final String serviceId;

    private Set<ServiceInstance> serviceInstanceSet;

    private static Map<String, RandomLoadBalanceStrategy> map = new ConcurrentHashMap<>();

    public static RandomLoadBalanceStrategy getInstance(String serviceId){
        RandomLoadBalanceStrategy randomLoadBalanceRule = map.get(serviceId);
        if (randomLoadBalanceRule == null) {
            randomLoadBalanceRule = new RandomLoadBalanceStrategy(serviceId);
            map.put(serviceId, randomLoadBalanceRule);
        }
        return randomLoadBalanceRule;
    }


    private RandomLoadBalanceStrategy(String serviceId) {
        this.serviceId = serviceId;
        this.serviceInstanceSet = DynamicConfigManager.getInstance().getServiceInstanceByUniqueId(serviceId, true);
    }

    @Override
    public ServiceInstance choose(GatewayContext ctx) {
        String serviceId = ctx.getUniqueId();
        return choose(serviceId, ctx.isGray());
    }

    @Override
    public ServiceInstance choose(String serviceId, boolean gray) {
        if (serviceInstanceSet.isEmpty()){
            serviceInstanceSet = DynamicConfigManager.getInstance().getServiceInstanceByUniqueId(serviceId, gray);
        }
        if (serviceInstanceSet.isEmpty()){
            log.warn("No instance available for: {}", serviceId);
            throw new NotFoundException(ResponseCode.SERVICE_INSTANCE_NOT_FOUND);
        }
        ArrayList<ServiceInstance> serviceInstances = new ArrayList<>(serviceInstanceSet);
        int index = ThreadLocalRandom.current().nextInt(serviceInstances.size());
        return serviceInstances.get(index);
    }
}
