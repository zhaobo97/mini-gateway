package org.zhaobo.core.filter.monitor;

import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.zhaobo.core.context.GatewayContext;
import org.zhaobo.core.filter.Filter;
import org.zhaobo.core.filter.FilterInfo;
import org.zhaobo.common.constants.FilterConst;

/**
 * @Auther: bo
 * @Date: 2023/12/9 14:49
 * @Description:
 */
@Slf4j
@FilterInfo(id = FilterConst.MONITOR_FILTER_ID,
name = FilterConst.MONITOR_FILTER_NAME,
order = FilterConst.MONITOR_FILTER_ORDER)
public class MonitorFilter implements Filter {
    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        ctx.setTimerSample(Timer.start());
    }
}
