package org.zhaobo.ping.core.filter.gray;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.zhaobo.ping.common.constants.FilterConst;
import org.zhaobo.ping.core.context.GatewayContext;
import org.zhaobo.ping.core.filter.Filter;
import org.zhaobo.ping.core.filter.FilterInfo;

/**
 * @Auther: bo
 * @Date: 2023/12/9 13:44
 * @Description:
 */
@Slf4j
@FilterInfo(id = FilterConst.GRAY_FILTER_ID,
        name = FilterConst.GRAY_FILTER_NAME,
        order = FilterConst.GRAY_FILTER_ORDER)
public class GrayFilter implements Filter {
    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        // 测试灰度功能待时候使用，可以手动指定为灰度流量
        String gray = ctx.getRequest().getHttpHeaders().get("gray");
        if (StringUtils.equalsIgnoreCase("true", gray)){
            ctx.setGray(true);
        }
        // 如果不是手动测试，选取1024分之一的用户作为灰度流量
        String clientIP = ctx.getRequest().getClientIP();
        int res = clientIP.hashCode() & (1024 - 1);
        if (res == 1) {
            ctx.setGray(true);
        }
    }
}
