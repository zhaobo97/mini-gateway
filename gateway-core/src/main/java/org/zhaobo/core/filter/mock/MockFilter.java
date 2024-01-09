package org.zhaobo.core.filter.mock;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.zhaobo.core.context.GatewayContext;
import org.zhaobo.core.filter.FilterInfo;
import org.zhaobo.core.helper.ResponseHelper;
import org.zhaobo.core.response.GatewayResponse;
import org.zhaobo.common.config.Rule;
import org.zhaobo.common.constants.FilterConst;
import org.zhaobo.core.filter.Filter;

import java.util.Map;

/**
 * @Auther: bo
 * @Date: 2023/12/9 17:38
 * @Description:
 */
@Slf4j
@FilterInfo(id = FilterConst.MONITOR_END_FILTER_ID,
        name = FilterConst.MONITOR_END_FILTER_NAME,
        order = FilterConst.MONITOR_END_FILTER_ORDER)
public class MockFilter implements Filter {
    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        Rule.FilterConfig filterConfig = ctx.getRule().getFilterConfig(FilterConst.MONITOR_END_FILTER_ID);
        if (filterConfig == null) return;
        Map<String, String> map = JSON.parseObject(filterConfig.getConfig(), Map.class);
        String value = map.get(ctx.getRequest().getHttpMethod() + " " + ctx.getRequest().getPath());
        if (value != null) {
            ctx.setResponse(GatewayResponse.buildOnSuccess(value));
            ctx.setWritten();
            ResponseHelper.writeResponse(ctx);
            log.info("mock {} {} {}", ctx.getRequest().getHttpMethod(), ctx.getRequest().getPath(), value);
            ctx.setTerminated();
        }
    }
}
