package org.zhaobo.config.center.nacos;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.extern.slf4j.Slf4j;
import org.zhaobo.config.center.api.ConfigCenter;
import org.zhaobo.config.center.api.RulesChangeListener;
import org.zhaobo.common.config.Rule;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * @Auther: bo
 * @Date: 2023/12/4 21:55
 * @Description:
 */
@Slf4j
public class NacosConfigCenter implements ConfigCenter {

    private static final String DATA_ID = "api-gateway";

    private String serverAddress;

    private String namespace;

    private String group;

    private ConfigService configService;

    @Override
    public void init(String serverAddress, String namespace, String group, String username, String password) {
        this.namespace = namespace;
        this.group = group;
        try {
            Properties properties = new Properties();
            //设置注册地址
            properties.setProperty(PropertyKeyConst.SERVER_ADDR,serverAddress);
            //设置命名空间
            properties.setProperty(PropertyKeyConst.NAMESPACE,namespace);
            properties.setProperty(PropertyKeyConst.USERNAME,username);
            properties.setProperty(PropertyKeyConst.PASSWORD,password);
            this.configService = ConfigFactory.createConfigService(properties);
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void subscribeRulesChange(RulesChangeListener listener) {
        try {
            // 初始化通知
            String config = configService.getConfig(DATA_ID, group, 5000);
            log.info("config from nacos : {}", config);
            //{"rules":[{Rule}, {}]}
            List<Rule> rules = JSON.parseObject(config).getJSONArray("rules").toJavaList(Rule.class);
            listener.onChange(rules);

            // 监听变化
            configService.addListener(DATA_ID, group, new Listener() {
                @Override
                public Executor getExecutor() {
                    return null;
                }

                @Override
                public void receiveConfigInfo(String configInfo) {
                    log.info("config from nacos : {}", configInfo);
                    List<Rule> rules = JSON.parseObject(configInfo)
                            .getJSONArray("rules").toJavaList(Rule.class);
                    listener.onChange(rules);
                }
            });
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }
}
