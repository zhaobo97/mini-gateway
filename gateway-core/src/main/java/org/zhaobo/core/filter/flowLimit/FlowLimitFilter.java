package org.zhaobo.core.filter.flowLimit;

import lombok.extern.slf4j.Slf4j;
import org.zhaobo.core.context.GatewayContext;
import org.zhaobo.core.filter.Filter;
import org.zhaobo.core.filter.FilterInfo;
import org.zhaobo.common.config.Rule;
import org.zhaobo.common.constants.FilterConst;

import java.util.Iterator;
import java.util.Set;


/**
 * @Auther: bo
 * @Date: 2023/12/7 19:06
 * @Description: 限流过滤器
 */
@Slf4j
@FilterInfo(id = FilterConst.FLOW_LIMIT_FILTER_ID,
        name = FilterConst.FLOW_LIMIT_FILTER_NAME,
        order = FilterConst.FLOW_LIMIT_FILTER_ORDER
)
public class FlowLimitFilter implements Filter {

    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        Rule rule = ctx.getRule();
        if (rule != null) {
            Set<Rule.FlowLimitConfig> set = rule.getFlowLimitConfig();
            Iterator<Rule.FlowLimitConfig> iterator = set.iterator();
            Rule.FlowLimitConfig flowLimitConfig;
            while (iterator.hasNext()){
                IGatewayFlowLimitStrategy flowLimitStrategy = null;
                flowLimitConfig = iterator.next();
                if (flowLimitConfig == null) continue;
                String path = ctx.getRequest().getPath();
                if (flowLimitConfig.getType().equalsIgnoreCase(FilterConst.FLOW_LIMIT_TYPE_PATH)
                && path.equals(flowLimitConfig.getValue())) {
                    flowLimitStrategy = FlowLimitByPathStrategy.getInstance(rule.getServiceId(), path);
                }else if (flowLimitConfig.getType().equalsIgnoreCase(FilterConst.FLOW_LIMIT_TYPE_SERVICE)){

                }
                if (flowLimitStrategy != null){
                    // 执行限流过滤器
                    flowLimitStrategy.doLimit(flowLimitConfig, rule.getServiceId());
                }
            }
        }
    }

}
