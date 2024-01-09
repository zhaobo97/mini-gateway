package org.zhaobo.common.constants;

/**
 * @Auther: bo
 * @Date: 2023/12/5 21:42
 * @Description: 负载均衡常量
 */

public interface FilterConst {

    //---------------------------- 监控过滤器（开始） ---------------------------------------
    String MONITOR_FILTER_ID = "monitor_filter";
    String MONITOR_FILTER_NAME = "monitor_filter";
    int MONITOR_FILTER_ORDER = -1;
    //---------------------------- 灰度发布过滤器 ---------------------------------------
    String GRAY_FILTER_ID = "gray_filter";
    String GRAY_FILTER_NAME = "gray_filter";
    int GRAY_FILTER_ORDER = 10;
    //---------------------------- 用户验证过滤器 ---------------------------------------
    String AUTH_FILTER_ID = "auth_filter";
    String AUTH_FILTER_NAME = "auth_filter";
    int AUTH_FILTER_ORDER = 20;
    // ---------------------------- 限流过滤器 ------------------------------------------
    String FLOW_LIMIT_FILTER_ID = "flow_limit_filter";
    String FLOW_LIMIT_FILTER_NAME = "flow_limit_filter";
    int FLOW_LIMIT_FILTER_ORDER = 30;
    String FLOW_LIMIT_TYPE_PATH = "path";
    String FLOW_LIMIT_TYPE_SERVICE = "service";
    String FLOW_LIMIT_DURATION = "duration"; // 限流间隔， 以秒为单位
    String FLOW_LIMIT_PERMITS = "permits"; // 令牌数
    String FLOW_LIMIT_MODEL_DISTRIBUTED = "distributed"; // 分布式
    String FLOW_LIMIT_MODEL_SINGLETON = "singleton";
    // -------------------------- 负载均衡过滤器 ------------------------------------------
    String LOAD_BALANCE_FILTER_ID = "load_balance_filter";
    String LOAD_BALANCE_FILTER_NAME = "load_balance_filter";
    int LOAD_BALANCE_FILTER_ORDER = 40;
    String LOAD_BALANCE_KEY = "load_balance";
    String LOAD_BALANCE_STRATEGY_RANDOM = "random";
    String LOAD_BALANCE_STRATEGY_ROUND_ROBIN = "round_robin";
    //------------------------------路由过滤器-------------------------------------
    String ROUTER_FILTER_ID = "router_filter";
    String ROUTER_FILTER_NAME = "router_filter";
    int ROUTER_FILTER_ORDER = Integer.MAX_VALUE;
    //----------------------------监控过滤器(结束)---------------------------------------
    String MONITOR_END_FILTER_ID = "monitor_end_filter";
    String MONITOR_END_FILTER_NAME = "monitor_end_filter";
    int MONITOR_END_FILTER_ORDER = 50;
    //----------------------------模拟过滤器---------------------------------------
    String MOCK_FILTER_ID = "mock_filter";
    String MOCK_FILTER_NAME = "mock_filter";
    int MOCK_FILTER_ORDER = 0;
}
