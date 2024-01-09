package org.zhaobo.ping.core.filter.flowLimit.limiter;

import com.google.common.util.concurrent.RateLimiter;
import org.apache.commons.lang3.StringUtils;
import org.zhaobo.ping.common.config.Rule;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @Auther: bo
 * @Date: 2023/12/8 12:15
 * @Description: 单机限流
 */

public class GuavaLimiter {

    private final RateLimiter rateLimiter;
    private final double maxPermits;

    private static final ConcurrentHashMap<String , GuavaLimiter> map = new ConcurrentHashMap<>();

    public static GuavaLimiter getInstance(String serviceId, Rule.FlowLimitConfig config, int maxPermits){
        if(StringUtils.isEmpty(serviceId) || config ==null ||
                StringUtils.isEmpty(config.getValue()) ||
                StringUtils.isEmpty(config.getConfig()) ||
                StringUtils.isEmpty(config.getType())){
            return null;
        }
        StringBuffer sb = new StringBuffer();
        String key = sb.append(serviceId).append(".").append(config.getValue()).toString();
        GuavaLimiter guavaLimiter = map.get(key);
        if (guavaLimiter == null) {
            guavaLimiter = new GuavaLimiter(maxPermits);
            map.putIfAbsent(key, guavaLimiter);
        }
        return guavaLimiter;
    }

    public boolean acquire(int permit){
        return rateLimiter.tryAcquire(permit);
    }

    public GuavaLimiter(double maxPermits) {
        this.rateLimiter = RateLimiter.create(maxPermits);
        this.maxPermits = maxPermits;
    }

    public GuavaLimiter(long warmUpPeriodAsSecond, double maxPermits) {
        this.rateLimiter = RateLimiter.create(maxPermits, warmUpPeriodAsSecond, TimeUnit.SECONDS);
        this.maxPermits = maxPermits;
    }

}
