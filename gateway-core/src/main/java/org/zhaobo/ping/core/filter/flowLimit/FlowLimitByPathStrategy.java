package org.zhaobo.ping.core.filter.flowLimit;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.zhaobo.ping.common.config.Rule;
import org.zhaobo.ping.common.constants.FilterConst;
import org.zhaobo.ping.common.enums.ResponseCode;
import org.zhaobo.ping.common.exception.RateLimitException;
import org.zhaobo.ping.core.filter.flowLimit.limiter.GuavaLimiter;
import org.zhaobo.ping.core.filter.flowLimit.limiter.RedisLimiter;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Auther: bo
 * @Date: 2023/12/7 22:16
 * @Description: 根据路径限流
 */

public class FlowLimitByPathStrategy implements IGatewayFlowLimitStrategy {

    private final String serviceId;

    private final String path;

    private static final String LIMITED_MESSAGE = "您的请求过于频繁,请稍后重试";

    private static final ConcurrentHashMap<String, FlowLimitByPathStrategy> map = new ConcurrentHashMap<>();

    public static FlowLimitByPathStrategy getInstance(String serviceId, String path) {
        StringBuffer sb = new StringBuffer();
        String key = sb.append(serviceId).append(".").append(path).toString();
        FlowLimitByPathStrategy strategy = map.get(key);
        if (Objects.isNull(strategy)) {
            strategy = new FlowLimitByPathStrategy(serviceId, path);
            map.putIfAbsent(key, strategy);
        }
        return strategy;
    }

    public FlowLimitByPathStrategy(String serviceId, String path) {
        this.serviceId = serviceId;
        this.path = path;
    }

    @Override
    public void doLimit(Rule.FlowLimitConfig config, String serviceId) {
        if (config == null || StringUtils.isEmpty(serviceId) || StringUtils.isEmpty(config.getConfig())) {
            return;
        }
        // 校验json
        Map<String, Integer> configMap = JSON.parseObject(config.getConfig(), Map.class);
        if (configMap.get(FilterConst.FLOW_LIMIT_DURATION) == null ||
                configMap.get(FilterConst.FLOW_LIMIT_PERMITS) == null) {
            return;
        }
        boolean flag = true; // 单次是否通过
        String model = config.getModel();
        StringBuffer buffer = new StringBuffer();
        String key = buffer.append(serviceId).append(".").append(path).toString();
        if (FilterConst.FLOW_LIMIT_MODEL_DISTRIBUTED.equalsIgnoreCase(model)) {
            // 分布式限流
            Integer permits = configMap.get(FilterConst.FLOW_LIMIT_PERMITS);
            Integer duration = configMap.get(FilterConst.FLOW_LIMIT_DURATION);
            flag = RedisLimiter.getInstance().doLimit(key, permits, duration);
        } else {
            // 单机限流
            Integer maxPermits = configMap.get(FilterConst.FLOW_LIMIT_PERMITS);
            GuavaLimiter guavaLimiter = GuavaLimiter.getInstance(serviceId, config, maxPermits);
            if (guavaLimiter == null) {
                throw new RuntimeException("获取单机限流工具类为空");
            }
            flag = guavaLimiter.acquire(1);
        }
        if (!flag) {
            throw new RateLimitException(ResponseCode.RATE_LIMIT_ERROR.getMessage(), ResponseCode.RATE_LIMIT_ERROR);
        }
    }
}
