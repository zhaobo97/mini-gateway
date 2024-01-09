package org.zhaobo.core.filter.flowLimit;

import org.zhaobo.common.config.Rule;

/**
 * @Auther: bo
 * @Date: 2023/12/7 22:14
 * @Description:
 */
public interface IGatewayFlowLimitStrategy {

    void doLimit(Rule.FlowLimitConfig config, String serviceId);
}
