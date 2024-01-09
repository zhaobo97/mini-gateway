package org.zhaobo.ping.config.center.api;

/**
 * @Auther: bo
 * @Date: 2023/12/4 21:24
 * @Description:
 */

public interface ConfigCenter {
    void init(String serverAddress, String namespace, String group, String username, String password);

    void subscribeRulesChange(RulesChangeListener listener);
}
