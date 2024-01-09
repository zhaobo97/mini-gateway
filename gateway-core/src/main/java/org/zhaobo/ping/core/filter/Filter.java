package org.zhaobo.ping.core.filter;

import org.zhaobo.ping.core.context.GatewayContext;

/**
 * @Auther: bo
 * @Date: 2023/12/5 9:27
 * @Description: 过滤器顶级接口
 */
public interface Filter {

    void doFilter(GatewayContext ctx) throws Exception;

    default int getOrder(){
        FilterInfo annotation = this.getClass().getAnnotation(FilterInfo.class);
        if (annotation != null) {
            return annotation.order();
        }
        return Integer.MAX_VALUE;
    }
}
