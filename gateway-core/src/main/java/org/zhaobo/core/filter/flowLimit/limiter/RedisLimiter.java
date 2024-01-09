package org.zhaobo.core.filter.flowLimit.limiter;

import lombok.extern.slf4j.Slf4j;
import org.zhaobo.core.redis.JedisUtil;

/**
 * @Auther: bo
 * @Date: 2023/12/8 12:15
 * @Description: 分布式限流
 */
@Slf4j
public class RedisLimiter {

    protected JedisUtil jedisUtil;

    private static RedisLimiter redisLimiter ;

    public static RedisLimiter getInstance(){
        if (redisLimiter == null) redisLimiter = new RedisLimiter(new JedisUtil());
        return redisLimiter;
    }

    public RedisLimiter(JedisUtil jedisUtil) {
        this.jedisUtil = jedisUtil;
    }
    private static final int SUCCESS = 1;
    private static final int FAIL = 1;

    public boolean doLimit(String key, int limit, int expire){
        long value = 0;
        try {
            Object o = jedisUtil.executeScript(key, limit, expire);
            if (o == null) {
                return true;
            }
            value = Long.parseLong(o.toString());
        } catch (NumberFormatException e) {
            throw new RuntimeException("分布式限流出现异常:",e);
        }
        return FAIL == value;
    }
}
