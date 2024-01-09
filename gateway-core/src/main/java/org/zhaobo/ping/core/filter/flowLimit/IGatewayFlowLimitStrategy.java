package org.zhaobo.ping.core.filter.flowLimit;

import org.zhaobo.ping.common.config.Rule;

/**
 * @Auther: bo
 * @Date: 2023/12/7 22:14
 * @Description:
 */
public interface IGatewayFlowLimitStrategy {

    void doLimit(Rule.FlowLimitConfig config, String serviceId);
}
