package org.zhaobo.core.filter;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.zhaobo.core.context.GatewayContext;
import org.zhaobo.common.exception.BaseException;
import org.zhaobo.common.exception.RateLimitException;

import java.util.ArrayList;
import java.util.List;

/**
 * @Auther: bo
 * @Date: 2023/12/5 10:05
 * @Description: 过滤器链条类
 */
@Slf4j
public class GatewayFilterChain {

    private final List<Filter> filters = new ArrayList<>();


    public GatewayFilterChain addFilter(Filter filter){
        filters.add(filter);
        return this;
    }

    public GatewayFilterChain addFilters(List<Filter> filterList){
        filters.addAll(filterList);
        return this;
    }

    public GatewayContext doFilter(GatewayContext ctx) throws Exception {
        if (CollectionUtils.isEmpty(filters)){
            return ctx;
        }
        try {
            for (Filter filter : filters) {
                filter.doFilter(ctx);
                if (ctx.isTerminate()) {
                    break;
                }
            }
        } catch (Exception e) {
            if (e instanceof RateLimitException){
                throw (BaseException) e;
            }else {
                e.printStackTrace();
                log.error("doFilter error : {}", e.getMessage());
                throw new Exception(e);
            }
        }
        return ctx;
    }

}
