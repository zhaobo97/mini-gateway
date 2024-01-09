package org.zhaobo.ping.core.filter.loadbalance;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.zhaobo.ping.common.config.Rule;
import org.zhaobo.ping.common.config.ServiceInstance;
import org.zhaobo.ping.common.constants.BasicConst;
import org.zhaobo.ping.common.constants.FilterConst;
import org.zhaobo.ping.common.enums.ResponseCode;
import org.zhaobo.ping.common.exception.NotFoundException;
import org.zhaobo.ping.core.context.GatewayContext;
import org.zhaobo.ping.core.filter.Filter;
import org.zhaobo.ping.core.filter.FilterInfo;
import org.zhaobo.ping.core.request.GatewayRequest;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @Auther: bo
 * @Date: 2023/12/5 21:41
 * @Description:
 */
@Slf4j
@FilterInfo(id = FilterConst.LOAD_BALANCE_FILTER_ID,
        name = FilterConst.LOAD_BALANCE_FILTER_NAME,
        order = FilterConst.LOAD_BALANCE_FILTER_ORDER)
public class LoadBalanceFilter implements Filter {

    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        String serviceId = ctx.getUniqueId();
        IGatewayLoadBalanceStrategy gatewayLoadBalanceStrategy = getLoadBalanceStrategy(ctx);
        GatewayRequest request = ctx.getRequest();
        ServiceInstance instance = gatewayLoadBalanceStrategy.choose(serviceId, ctx.isGray());
        if (instance != null && request != null) {
            String host = instance.getIp() + BasicConst.COLON_SEPARATOR + instance.getPort();
            log.info(" -------------------- loadbalance choose: {} --------------", host);
            request.setModifyHost(host);
        }else {
            log.warn("No instance available for {}", serviceId);
            throw new NotFoundException(ResponseCode.SERVICE_INSTANCE_NOT_FOUND);
        }

    }

    /**
     * 根据上下文中的filter的配置，获取负载均衡策略
     * @param ctx
     * @return
     */
    private IGatewayLoadBalanceStrategy getLoadBalanceStrategy(GatewayContext ctx) {
        IGatewayLoadBalanceStrategy gatewayLoadBalanceRule = null;
        Rule rule = ctx.getRule();
        if (rule != null) {
            // 一个rule中有很多filter
            Set<Rule.FilterConfig> filterConfigs = rule.getFilterConfigs();
            Iterator<Rule.FilterConfig> iterator = filterConfigs.iterator();
            while (iterator.hasNext()) {
                Rule.FilterConfig filterConfig = iterator.next();
                if (filterConfig == null) {
                    continue;
                }
                // 先判断当前filter是否为负载均衡过滤器
                String filterConfigId = filterConfig.getId();
                if (!FilterConst.LOAD_BALANCE_FILTER_ID.equals(filterConfigId)) {
                    continue;
                }
                // config 是map序列化后的字符串
                String config = filterConfig.getConfig();
                String defaultStrategy = FilterConst.LOAD_BALANCE_STRATEGY_RANDOM;
                String strategy;
                if (StringUtils.isNotEmpty(config)) {
                    Map<String, String> map = JSON.parseObject(config, Map.class);
                    strategy = map.getOrDefault(FilterConst.LOAD_BALANCE_KEY, defaultStrategy);
                    switch (strategy) {
                        case FilterConst.LOAD_BALANCE_STRATEGY_RANDOM:
                            gatewayLoadBalanceRule = RandomLoadBalanceStrategy.getInstance(rule.getServiceId());
                            break;
                        case FilterConst.LOAD_BALANCE_STRATEGY_ROUND_ROBIN:
                            // 用单例才能保证始终只有一个position，如果每次都new一个实例，每次position都是初始值，
                            // 无法实现轮询
                            gatewayLoadBalanceRule = RoundRobinLoadBalanceStrategy.getInstance(rule.getServiceId());
                            break;
                        default:
                            log.warn("No loadbalance strategy flound for service {}", strategy);
                            gatewayLoadBalanceRule = RandomLoadBalanceStrategy.getInstance(rule.getServiceId());
                            break;
                    }
                }
            }
        }
        return gatewayLoadBalanceRule;
    }

    @Override
    public int getOrder() {
        return Filter.super.getOrder();
    }
}
