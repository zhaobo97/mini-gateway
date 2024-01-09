package org.zhaobo.ping.config.center.api;

import org.zhaobo.ping.common.config.Rule;

import java.util.List;

/**
 * @Auther: bo
 * @Date: 2023/12/4 21:26
 * @Description:
 */
public interface RulesChangeListener {

    void onChange(List<Rule> rules);
}
