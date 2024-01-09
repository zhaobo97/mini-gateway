package org.zhaobo.common.config;

import lombok.*;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @Auther: bo
 * @Date: 2023/12/1 21:47
 * @Description:
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "Id")
public class Rule implements Comparable<Rule>, Serializable {

    // 全局唯一ID
    private String Id;

    // 规则名称
    private String name;

    // 规则对应的协议
    private String protocol;

    // 规则优先级
    private Integer order;

    // 请求前缀
    private String prefix;

    // 后端服务id
    private String serviceId;

    // 路径集合
    private List<String> paths;

    private Set<FilterConfig> filterConfigs = new HashSet<>();

    private RetryConfig retryConfig = new RetryConfig();

    private Set<FlowLimitConfig> flowLimitConfig = new HashSet<>();

    private Set<HystrixConfig> hystrixConfigs = new HashSet<>();


    public boolean addFilterConfig(FilterConfig filterConfig){
        return this.filterConfigs.add(filterConfig);
    }

    public FilterConfig getFilterConfig(String id){
        for (FilterConfig filterConfig : filterConfigs) {
            if (filterConfig.getId().equals(id)) return filterConfig;
        }
        return null;
    }

    public boolean existFilterConfig(String id){
        for (FilterConfig filterConfig : filterConfigs) {
            if (filterConfig.getId().equals(id)) return true;
        }
        return false;
    }

    @Getter
    @Setter
    @EqualsAndHashCode(of = "id")
    public static class FilterConfig {
        // 规则配置ID
        private String id;
        // 配置信息
        private String config;
    }

    @Getter
    @Setter
    public static class RetryConfig{
        // 重试次数
        private int times;
    }

    @Getter
    @Setter
    public static class FlowLimitConfig {
        // 限流类型 - path / IP /service
        private String type;
        // 限流对象
        private String value;
        // 限流模式 - 单机 / 分布式
        private String model;
        // 限流规则
        private String config;
    }

    @Getter
    @Setter
    public static class HystrixConfig {
        private String path;
        private int timeout; // ms，超时熔断
        private int coreThreadSize;
        private String fallbackMessage;
        private boolean timeoutEnabled;

        private int slidingWindowDuration; //滑动窗口范围

        private int numberOfWindowSegments; //滑动窗口被分割的段数

        private int requestTimesThreshold;

        private int failureRateThreshold;

        private int circuitBreakerResetTime; //熔断时间

        private boolean circuitBreakerEnabled;

    }


    @Override
    public int compareTo(Rule o) {
        int compare = Integer.compare(getOrder(), o.getOrder());
        if(compare == 0) {
            return getId().compareTo(o.getId());
        }
        return compare;
    }
}
