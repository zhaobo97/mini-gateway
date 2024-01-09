package org.zhaobo.core.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.zhaobo.common.config.Rule;
import org.zhaobo.common.constants.FilterConst;
import org.zhaobo.core.context.GatewayContext;
import org.zhaobo.core.filter.route.RouterFilter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @Auther: bo
 * @Date: 2023/12/5 10:13
 * @Description: 过滤器工厂实现类
 */
@Slf4j
public class GatewayFilterChainFactory implements FilterChainFactory{

    public Map<String/*filter id*/, Filter> filterIdMap = new ConcurrentHashMap<>();

    //用Caffeine构建的本地缓存,缓存配置为每10分钟过期一次，并开启了统计功能（recordStats()）
    private final Cache<String/*rule id*/, GatewayFilterChain> filterChainCache = Caffeine.newBuilder()
            .recordStats().expireAfterWrite(10, TimeUnit.MINUTES).build(); // 10分钟后过期

    private static class Singleton{
        private static final GatewayFilterChainFactory INSTANCE = new GatewayFilterChainFactory();
    }

    public static GatewayFilterChainFactory getInstance(){
        return Singleton.INSTANCE;
    }

    /**
     * 用Java SPI 加载filter，实现可插拔
     */
    public GatewayFilterChainFactory() {
        ServiceLoader<Filter> serviceLoader = ServiceLoader.load(Filter.class);
        for (Filter filter : serviceLoader) {
            FilterInfo annotation = filter.getClass().getAnnotation(FilterInfo.class);
            log.info("load filter success: {}, {}, {}, {}", filter.getClass(),
                    annotation.id(), annotation.name(), annotation.order());
            if (annotation != null) {
                String filterId = annotation.id();
                // 添加到过滤器集合
                if (StringUtils.isEmpty(filterId)){
                    filterId = filter.getClass().getName();
                }
                filterIdMap.put(filterId, filter);
            }
        }
    }

    public GatewayFilterChain buildFilterChain(GatewayContext ctx) throws Exception {
        String ruleId = ctx.getRule().getId();
        return filterChainCache.get(ruleId, new Function<String, GatewayFilterChain>() {
            @Override
            public GatewayFilterChain apply(String s) {
                return doBuildFilterChain(ctx);
            }
        });
    }


    public GatewayFilterChain doBuildFilterChain(GatewayContext ctx) {
        GatewayFilterChain gatewayFilterChain = new GatewayFilterChain();
        ArrayList<Filter> filters = new ArrayList<>();
        Rule rule = ctx.getRule();
        // 所有请求都要走的filter，不必在配置中为每个请求都加上
        filters.add(getFilter(FilterConst.MONITOR_FILTER_ID));
        filters.add(getFilter(FilterConst.MONITOR_END_FILTER_ID));
        if (rule != null) {
            Set<Rule.FilterConfig> filterConfigs = rule.getFilterConfigs();

            Iterator<Rule.FilterConfig> iterator = filterConfigs.iterator();
            Rule.FilterConfig filterConfig = null;

            while (iterator.hasNext()){
                filterConfig = iterator.next();
                if (filterConfig == null) {
                    continue;
                }
                String filterId = filterConfig.getId();
                if (StringUtils.isNotEmpty(filterId)
                        && getFilter(filterId) != null) {
                    filters.add(getFilter(filterId));
                }
            }
        }
        // 添加路由过滤器 这是最后一个过滤器
        filters.add(new RouterFilter());
        // 排序后添加到chain中
        filters.sort(Comparator.comparingInt(Filter::getOrder));
        return gatewayFilterChain.addFilters(filters);
    }

    @Override
    public Filter getFilter(String filterId) {
        return filterIdMap.get(filterId);
    }
}
