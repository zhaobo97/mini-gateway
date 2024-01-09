package org.zhaobo.core.filter;

import org.zhaobo.core.context.GatewayContext;

/**
 * @Auther: bo
 * @Date: 2023/12/5 10:02
 * @Description: 工厂接口
 */
public interface FilterChainFactory {

    // 构建过滤器链条
    GatewayFilterChain buildFilterChain(GatewayContext ctx) throws Exception;

    // 通过过滤器Id获取过滤器
    Filter getFilter(String filterId) throws Exception;
}
